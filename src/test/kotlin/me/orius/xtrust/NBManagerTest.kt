package me.orius.xtrust

import me.orius.xtrust.core.NBManager
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class NBManagerTest {

    @Test
    fun `test resolve download url with version placeholder`() {
        val template = "https://github.com/netbirdio/netbird/releases/download/v{VERSION}/netbird_{VERSION}_linux_amd64.tar.gz"
        val resolved = NBManager.resolveDownloadUrl(template, "0.79.0")
        assertEquals(
            "https://github.com/netbirdio/netbird/releases/download/v0.79.0/netbird_0.79.0_linux_amd64.tar.gz",
            resolved
        )
    }

    @Test
    fun `test extract cli binary from tar gz`(@TempDir tempDir: Path) {
        val archiveFile = tempDir.resolve("netbird.tar.gz").toFile()
        val destinationBinary = tempDir.resolve("netbird").toFile()

        val dummyContent = "NETBIRD_LINUX_BINARY_CONTENT".toByteArray(StandardCharsets.UTF_8)

        TarArchiveOutputStream(GZIPOutputStream(Files.newOutputStream(archiveFile.toPath()))).use { tarOut ->
            // LICENSE
            val licBytes = "Dummy License".toByteArray(StandardCharsets.UTF_8)
            val licEntry = TarArchiveEntry("LICENSE")
            licEntry.size = licBytes.size.toLong()
            tarOut.putArchiveEntry(licEntry)
            tarOut.write(licBytes)
            tarOut.closeArchiveEntry()

            // netbird binary
            val binEntry = TarArchiveEntry("netbird")
            binEntry.size = dummyContent.size.toLong()
            tarOut.putArchiveEntry(binEntry)
            tarOut.write(dummyContent)
            tarOut.closeArchiveEntry()
        }

        NBManager.extractCliBinary(archiveFile, destinationBinary)

        assertTrue(destinationBinary.exists())
        assertEquals("NETBIRD_LINUX_BINARY_CONTENT", destinationBinary.readText())
    }
}
