package me.orius.xtrust.core

import me.orius.xtrust.XTrust
import me.orius.xtrust.config.ConfigManager
import me.orius.xtrust.config.types.NetbirdConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.zip.GZIPInputStream

class NBManager(private val plugin: XTrust) {

    @Volatile
    private var shouldRun = false

    @Volatile
    private var process: Process? = null

    var cliFile: File? = null
        private set

    fun setup() {
        val config = ConfigManager.getConfig<NetbirdConfig>()
        shouldRun = true
        plugin.logger.info("Initializing Netbird CLI...")

        plugin.foliaLib.scheduler.runAsync {
            try {
                val binary = getOrMakeExecutable(config).also { cliFile = it }
                plugin.logger.info("Netbird CLI ready at: ${binary.absolutePath}")
                loop(config)
            } catch (e: Exception) {
                failSetup("Failed to download or run Netbird CLI", e)
            }
        }
    }

    fun shutdown() {
        if (!shouldRun) return
        shouldRun = false
        plugin.logger.info("Shutting down Netbird...")
        runCatching {
            process?.apply {
                destroy()
                if (!waitFor(3, TimeUnit.SECONDS)) destroyForcibly()
            }
        }
        process = null
        killLeftoverProcess()
    }

    fun restart(callback: (() -> Unit)? = null) {
        plugin.foliaLib.scheduler.runAsync {
            shutdown()
            setup()
            callback?.invoke()
        }
    }

    private fun failSetup(message: String, e: Exception) {
        plugin.logger.log(Level.SEVERE, message, e)
        shouldRun = false
    }

    private fun getCacheDir(config: NetbirdConfig): File {
        val configured = config.download.cacheDir.ifBlank { DEFAULT_CACHE_DIR }
        val base = File(configured)

        return if (base.isDirectory) base
        else plugin.dataFolder.resolve(configured).apply { mkdirs() }
    }

    private fun getOrMakeExecutable(config: NetbirdConfig): File {
        val cacheDir = getCacheDir(config)
        val targetBinary = File(cacheDir, "netbird-${config.download.version}")

        return if (targetBinary.isFile && targetBinary.length() > 0) {
            targetBinary.apply(::ensureExecutable)
        } else {
            downloadExecutable(config, targetBinary, cacheDir)
        }
    }

    private fun downloadExecutable(config: NetbirdConfig, targetBinary: File, cacheDir: File): File {
        val downloadUrl = resolveDownloadUrl(config.download.url, config.download.version)
        plugin.logger.info("Downloading Netbird CLI (v${config.download.version}) from $downloadUrl ...")

        val tempArchive = File(cacheDir, "netbird_download_${System.currentTimeMillis()}.tar.gz")
        val tempBinary = File(cacheDir, "netbird_extract_${System.currentTimeMillis()}.tmp")

        return try {
            downloadArchive(downloadUrl, tempArchive)
            plugin.logger.info("Download completed (${tempArchive.length()} bytes). Extracting Netbird binary...")

            extractCliBinary(tempArchive, tempBinary)

            Files.move(
                tempBinary.toPath(),
                targetBinary.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            targetBinary.apply(::ensureExecutable).also {
                cleanCacheDirExcept(cacheDir, targetBinary)
                plugin.logger.info("Extracted Netbird CLI successfully: ${targetBinary.absolutePath}")
            }
        } finally {
            tempArchive.delete()
            tempBinary.delete()
        }
    }

    private fun downloadArchive(urlStr: String, destinationFile: File) {
        destinationFile.parentFile?.mkdirs()
        val conn = (URI.create(urlStr).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            instanceFollowRedirects = true
        }
        if (conn.responseCode !in 200..299) {
            throw IOException("HTTP download failed with status ${conn.responseCode} from $urlStr")
        }
        conn.inputStream.use { input ->
            destinationFile.outputStream().use(input::transferTo)
        }
    }

    private fun cleanCacheDirExcept(cacheDir: File, keepBinary: File) = runCatching {
        cacheDir.listFiles { file ->
            file.isFile && file != keepBinary && (file.name.startsWith("netbird") || file.name.endsWith(".tar.gz") || file.name.endsWith(
                ".tmp"
            ))
        }?.forEach(File::delete)
    }

    private fun ensureExecutable(file: File) = with(file) {
        if (!canExecute()) setExecutable(true, false)
        if (!canRead()) setReadable(true, false)
    }

    private fun spawn(config: NetbirdConfig, binary: File): Process? = runCatching {
        val cmd = buildList {
            add(binary.absolutePath)
            add("up")
            if (config.setupKey.isNotBlank()) {
                add("--setup-key")
                add(config.setupKey)
            }
        }

        plugin.logger.info("Starting Netbird process: ${cmd.joinToString(" ")}")
        ProcessBuilder(cmd).redirectErrorStream(true).start().also { proc ->
            plugin.foliaLib.scheduler.runAsync {
                runCatching {
                    proc.inputStream.bufferedReader().forEachLine { line ->
                        plugin.logger.info("[NetBird] $line")
                    }
                }
            }
        }
    }.onFailure { plugin.logger.log(Level.SEVERE, "Failed to spawn Netbird process", it) }.getOrNull()

    private fun loop(config: NetbirdConfig) {
        if (!shouldRun) return
        val binary = cliFile ?: return
        val proc = spawn(config, binary) ?: return triggerRestart(config, "Spawning Netbird failed, will retry...")

        process = proc
        proc.onExit().thenRun { onNetbirdExit(proc, config) }
    }

    private fun onNetbirdExit(proc: Process, config: NetbirdConfig) {
        if (process === proc) process = null
        plugin.logger.info("Netbird process exited with code ${proc.exitValue()}")
        triggerRestart(config, "Auto-restart enabled. Restarting Netbird...")
    }

    private fun triggerRestart(config: NetbirdConfig, message: String) {
        if (!shouldRun) return

        if (config.restart.autoRestart) {
            plugin.logger.info(message)
            val delaySeconds = config.restart.restartAfter.coerceAtLeast(1).toLong()
            plugin.foliaLib.scheduler.runLaterAsync({ _ ->
                if (shouldRun) loop(config)
            }, delaySeconds, TimeUnit.SECONDS)
        } else {
            plugin.logger.info("Auto-restart is disabled.")
        }
    }

    private fun killLeftoverProcess() = runCatching {
        val p = ProcessBuilder("pkill", "-f", "netbird").start()
        if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
            plugin.logger.info("Cleaned up lingering netbird processes.")
        }
    }

    companion object {
        private const val DEFAULT_CACHE_DIR = ".cache/xtrust/netbird"

        fun resolveDownloadUrl(urlTemplate: String, version: String): String = urlTemplate.replace("{VERSION}", version)

        fun extractCliBinary(archiveFile: File, destinationBinary: File) {
            val found = TarArchiveInputStream(GZIPInputStream(archiveFile.inputStream().buffered())).use { tar ->
                generateSequence { tar.nextEntry }.filterNot { it.isDirectory }.any { entry ->
                    if (entry.name.substringAfterLast('/').substringAfterLast('\\') == "netbird") {
                        destinationBinary.outputStream().use(tar::copyTo)
                        true
                    } else false
                }
            }
            if (!found) {
                throw IOException("Binary 'netbird' not found in downloaded archive: ${archiveFile.name}")
            }
        }
    }
}