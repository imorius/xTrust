package me.orius.xtrust.config

import de.exlll.configlib.NameFormatters
import de.exlll.configlib.YamlConfigurationProperties
import de.exlll.configlib.YamlConfigurations
import me.orius.xtrust.XTrust
import me.orius.xtrust.annotations.File as ConfigFile
import me.orius.xtrust.config.types.NetbirdConfig
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

@Suppress("UNCHECKED_CAST")
class ConfigManager(private val plugin: XTrust) {

    companion object {
        lateinit var instance: ConfigManager
            private set

        inline fun <reified T : Any> getConfig(): T = instance.getConfig(T::class.java)

        inline fun <reified T : Any> saveConfig() = instance.saveConfig(T::class.java)
    }

    private val configs = ConcurrentHashMap<Class<*>, Any>()

    private val configClasses = listOf(
        NetbirdConfig::class.java
    )

    private val properties =
        YamlConfigurationProperties.newBuilder().setNameFormatter(NameFormatters.LOWER_KEBAB_CASE).build()

    init {
        instance = this
        registerAll()
    }


    fun <T : Any> getConfig(clazz: Class<T>): T = configs[clazz] as T

    inline fun <reified T : Any> getConfig(): T = getConfig(T::class.java)

    fun <T : Any> saveConfig(clazz: Class<T>) {
        val config = configs[clazz] ?: return
        val path = getPath(clazz)
        try {
            YamlConfigurations.save(path, clazz, clazz.cast(config), properties)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save config ${clazz.simpleName} to $path", e)
        }
    }

    inline fun <reified T : Any> saveConfig() = saveConfig(T::class.java)

    fun registerConfig(clazz: Class<*>) {
        val path = getPath(clazz)

        try {
            val config = YamlConfigurations.update(path, clazz, properties)
            configs[clazz] = config
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load config ${clazz.simpleName} from $path", e)
        }
    }

    fun registerAll() {
        configClasses.forEach {
            registerConfig(it)
        }
    }

    fun reloadAll() {
        configs.clear()
        registerAll();
    }

    private fun getPath(clazz: Class<*>): Path {
        val annotation = clazz.getAnnotation(ConfigFile::class.java)
            ?: error("Config class ${clazz.simpleName} must be annotated with @File")
        return plugin.dataFolder.toPath().resolve(annotation.value)
    }
}
