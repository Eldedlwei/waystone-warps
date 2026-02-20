package dev.mizarc.waystonewarps.interaction.commands

import dev.mizarc.waystonewarps.application.actions.administration.ListInvalidWarps
import dev.mizarc.waystonewarps.application.actions.administration.RemoveAllInvalidWarps
import dev.mizarc.waystonewarps.application.actions.administration.RemoveInvalidWarpsForWorld
import dev.mizarc.waystonewarps.interaction.localization.LocalizationKeys
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import java.util.UUID

class InvalidsCommand : KoinComponent {
    private val listInvalidWarps: ListInvalidWarps by inject()
    private val removeAllInvalidWarps: RemoveAllInvalidWarps by inject()
    private val removeInvalidWarpsForWorld: RemoveInvalidWarpsForWorld by inject()
    private val localizationProvider: LocalizationProvider by inject()

    fun register(commands: Commands) {
        commands.register(
            "waystonewarps",
            "WaystoneWarps admin commands",
            listOf("ww"),
            object : BasicCommand {
                override fun execute(commandSourceStack: CommandSourceStack, args: Array<String>) {
                    val sender = commandSourceStack.getSender()
                    if (args.isEmpty()) {
                        sendUsage(sender)
                        return
                    }

                    if (!args[0].equals("invalids", ignoreCase = true)) {
                        sendUsage(sender)
                        return
                    }

                    if (args.size == 1) {
                        sendInvalidsUsage(sender)
                        return
                    }

                    when (args[1].lowercase(Locale.ENGLISH)) {
                        "list" -> {
                            if (!sender.hasPermission("waystonewarps.admin.invalids.list")) {
                                sendNoPermission(sender)
                                return
                            }
                            onListInvalids(sender)
                        }

                        "remove" -> {
                            if (!sender.hasPermission("waystonewarps.admin.invalids.remove")) {
                                sendNoPermission(sender)
                                return
                            }
                            if (args.size < 3) {
                                sender.sendMessage(Component.text("Usage: /ww invalids remove <world>", NamedTextColor.RED))
                                return
                            }
                            onRemoveInvalids(sender, args[2])
                        }

                        "removeall" -> {
                            if (!sender.hasPermission("waystonewarps.admin.invalids.removeall")) {
                                sendNoPermission(sender)
                                return
                            }
                            onRemoveAllInvalids(sender)
                        }

                        else -> sendInvalidsUsage(sender)
                    }
                }

                override fun suggest(commandSourceStack: CommandSourceStack, args: Array<String>): Collection<String> {
                    if (args.isEmpty()) return listOf("invalids")

                    if (args.size == 1) {
                        return listOf("invalids").filter { it.startsWith(args[0], ignoreCase = true) }
                    }

                    if (!args[0].equals("invalids", ignoreCase = true)) return emptyList()

                    if (args.size == 2) {
                        return listOf("list", "remove", "removeall")
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }

                    if (args.size == 3 && args[1].equals("remove", ignoreCase = true)) {
                        val worlds = Bukkit.getWorlds().flatMap { world -> listOf(world.name, world.uid.toString()) }
                        return worlds.filter { it.startsWith(args[2], ignoreCase = true) }
                    }

                    return emptyList()
                }
            }
        )
    }

    private fun sendNoPermission(sender: CommandSender) {
        sender.sendMessage(Component.text("You do not have permission to run this command.", NamedTextColor.RED))
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(Component.text("Usage: /ww invalids <list|remove|removeall>", NamedTextColor.YELLOW))
    }

    private fun sendInvalidsUsage(sender: CommandSender) {
        sender.sendMessage(Component.text("/ww invalids list", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/ww invalids remove <world>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/ww invalids removeall", NamedTextColor.YELLOW))
    }

    private fun onListInvalids(sender: CommandSender) {
        val warps = listInvalidWarps.listAllInvalidWarps()

        if (warps.isEmpty()) {
            val message = if (sender is Player) {
                localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_NO_INVALID_WARPS)
            } else {
                localizationProvider.getConsole(LocalizationKeys.COMMAND_INVALIDS_NO_INVALID_WARPS)
            }
            sender.sendMessage(Component.text(message, NamedTextColor.GREEN))
            return
        }

        val warpsByWorld = warps.groupBy { it.worldId }
            .mapValues { (_, warpsInWorld) -> warpsInWorld.size }
            .toList()
            .sortedByDescending { (_, count) -> count }

        val headerMessage = if (sender is Player) {
            localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_LIST_HEADER, warps.size)
        } else {
            localizationProvider.getConsole(LocalizationKeys.COMMAND_INVALIDS_LIST_HEADER, warps.size)
        }
        sender.sendMessage(Component.text(headerMessage, NamedTextColor.GOLD))

        warpsByWorld.forEach { (worldId, count) ->
            val worldIdStr = worldId.toString()
            val worldEntryMessage = if (sender is Player) {
                localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_LIST_WORLD_ENTRY, worldIdStr, count)
            } else {
                localizationProvider.getConsole(LocalizationKeys.COMMAND_INVALIDS_LIST_WORLD_ENTRY, worldIdStr, count)
            }

            val baseMessage = Component.text(worldEntryMessage, NamedTextColor.GRAY)
            if (sender is Player) {
                val hoverText = localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_LIST_CLIPBOARD_HOVER)
                sender.sendMessage(
                    baseMessage
                        .clickEvent(ClickEvent.copyToClipboard(worldIdStr))
                        .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.GREEN)))
                )
            } else {
                sender.sendMessage(baseMessage)
            }
        }
    }

    private fun onRemoveInvalids(sender: CommandSender, worldId: String) {
        try {
            val uuid = try {
                UUID.fromString(worldId)
            } catch (_: IllegalArgumentException) {
                val world = Bukkit.getWorld(worldId)
                if (world == null) {
                    val errorMessage = if (sender is Player) {
                        localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_REMOVE_INVALID_WORLD, worldId)
                    } else {
                        localizationProvider.getConsole(LocalizationKeys.COMMAND_INVALIDS_REMOVE_INVALID_WORLD, worldId)
                    }
                    sender.sendMessage(Component.text(errorMessage, NamedTextColor.RED))
                    return
                }
                world.uid
            }

            val (removed, _) = removeInvalidWarpsForWorld.execute(uuid)
            val world = Bukkit.getWorld(uuid)
            val worldName = world?.name ?: "Unknown World ($uuid)"
            val successMessage = if (sender is Player) {
                localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_REMOVE_SUCCESS, removed, worldName)
            } else {
                localizationProvider.getConsole(LocalizationKeys.COMMAND_INVALIDS_REMOVE_SUCCESS, removed, worldName)
            }
            sender.sendMessage(Component.text(successMessage, NamedTextColor.GREEN))
        } catch (e: Exception) {
            val errorMessage = if (sender is Player) {
                localizationProvider.get(sender.uniqueId, LocalizationKeys.COMMAND_INVALIDS_REMOVE_ERROR, e.message ?: "Unknown error")
            } else {
                localizationProvider.getConsole(LocalizationKeys.COMMAND_INVALIDS_REMOVE_ERROR, e.message ?: "Unknown error")
            }
            sender.sendMessage(Component.text(errorMessage, NamedTextColor.RED))
        }
    }

    private fun onRemoveAllInvalids(sender: CommandSender) {
        val (removed, _) = removeAllInvalidWarps.execute()
        val message = if (sender is Player) {
            localizationProvider.get(sender.uniqueId, "command.invalids.remove_all.success", removed)
        } else {
            localizationProvider.getConsole("command.invalids.remove_all.success", removed)
        }
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN))
    }
}
