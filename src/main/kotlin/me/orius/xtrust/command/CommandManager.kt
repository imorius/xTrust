package me.orius.xtrust.command

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.executors.CommandExecutor
import me.orius.xtrust.XTrust

class CommandManager(private val plugin: XTrust) {

    fun registerCommands() {
        registerRootCommand()
    }

    fun unregisterCommands() {
        CommandAPI.unregister("xtrust")
    }

    private fun registerRootCommand() {
        CommandAPICommand("xtrust")
            .withAliases("xt")
            .withPermission(CommandPermission.fromString("xtrust.admin"))
            .withHelp("xTrust main command", "Manage xTrust plugin and its services")
            .withSubcommand(ReloadCommand(plugin).createCommand())
            .executes(CommandExecutor { sender, _ ->
                sender.sendRichMessage("<gold><bold>xTrust</bold></gold> <dark_gray>| <gray>Version <yellow>${plugin.pluginMeta.version}</yellow>")
                sender.sendRichMessage("<yellow>/xtrust reload [config|netbird|all]</yellow> <gray>- Reload plugin configuration / NetBird</gray>")
            })
            .register(plugin)
    }
}
