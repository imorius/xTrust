package me.orius.xtrust

import com.tcoded.folialib.FoliaLib
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIPaperConfig
import me.orius.xtrust.command.CommandManager
import me.orius.xtrust.config.ConfigManager
import me.orius.xtrust.core.NBManager
import org.bukkit.plugin.java.JavaPlugin

class XTrust : JavaPlugin() {

    lateinit var foliaLib: FoliaLib
        private set

    lateinit var configManager: ConfigManager
        private set

    lateinit var nbManager: NBManager
        private set

    lateinit var commandManager: CommandManager
        private set

    val netbirdManager: NBManager
        get() = nbManager

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIPaperConfig(this).verboseOutput(false))
    }

    override fun onEnable() { // Plugin startup logic
        CommandAPI.onEnable()

        foliaLib = FoliaLib(this)
        configManager = ConfigManager(this)
        nbManager = NBManager(this)
        nbManager.setup()

        commandManager = CommandManager(this)
        commandManager.registerCommands()
    }

    override fun onDisable() { // Plugin shutdown logic
        if (::commandManager.isInitialized) {
            commandManager.unregisterCommands()
        }
        if (::nbManager.isInitialized) {
            nbManager.shutdown()
        }
        if (::foliaLib.isInitialized) {
            foliaLib.scheduler.cancelAllTasks()
        }
        CommandAPI.onDisable()
    }
}
