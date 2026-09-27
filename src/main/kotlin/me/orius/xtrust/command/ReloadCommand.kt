package me.orius.xtrust.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.executors.CommandExecutor
import me.orius.xtrust.XTrust
import org.bukkit.command.CommandSender
import java.util.logging.Level

class ReloadCommand(private val plugin: XTrust) {

    fun createCommand(): CommandAPICommand {
        return CommandAPICommand("reload")
            .withPermission(CommandPermission.fromString("xtrust.command.reload"))
            .withHelp("Reload configuration or NetBird", "Reloads plugin configuration files and/or NetBird client")
            .executes(CommandExecutor { sender, _ ->
                reloadConfig(sender)
            })
            .withSubcommand(
                CommandAPICommand("config")
                    .withPermission(CommandPermission.fromString("xtrust.command.reload"))
                    .withHelp("Reload configuration", "Reloads plugin configuration files")
                    .executes(CommandExecutor { sender, _ ->
                        reloadConfig(sender)
                    })
            )
            .withSubcommand(
                CommandAPICommand("netbird")
                    .withPermission(CommandPermission.fromString("xtrust.command.reload"))
                    .withHelp("Restart NetBird", "Restarts NetBird process")
                    .executes(CommandExecutor { sender, _ ->
                        reloadNetbird(sender)
                    })
            )
            .withSubcommand(
                CommandAPICommand("all")
                    .withPermission(CommandPermission.fromString("xtrust.command.reload"))
                    .withHelp("Reload all", "Reloads configuration and restarts NetBird")
                    .executes(CommandExecutor { sender, _ ->
                        reloadAll(sender)
                    })
            )
    }

    private fun reloadConfig(sender: CommandSender) {
        sender.sendRichMessage("<yellow>[xTrust]</yellow> <gray>Reloading configuration files...</gray>")
        try {
            plugin.configManager.reloadAll()
            sender.sendRichMessage("<green>[xTrust]</green> <green>Configuration files reloaded successfully!</green>")
            plugin.logger.info("Plugin configuration reloaded by ${sender.name}")
        } catch (e: Exception) {
            sender.sendRichMessage("<red>[xTrust]</red> <red>Failed to reload configuration: ${e.message}</red>")
            plugin.logger.log(Level.SEVERE, "Failed to reload configuration", e)
        }
    }

    private fun reloadNetbird(sender: CommandSender) {
        sender.sendRichMessage("<yellow>[xTrust]</yellow> <gray>Restarting NetBird daemon...</gray>")
        plugin.netbirdManager.restart {
            sender.sendRichMessage("<green>[xTrust]</green> <green>NetBird daemon restarted successfully!</green>")
        }
    }

    private fun reloadAll(sender: CommandSender) {
        reloadConfig(sender)
        reloadNetbird(sender)
    }
}
