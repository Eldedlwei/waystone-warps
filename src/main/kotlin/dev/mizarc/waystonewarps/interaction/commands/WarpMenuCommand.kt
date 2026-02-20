package dev.mizarc.waystonewarps.interaction.commands

import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import dev.mizarc.waystonewarps.interaction.menus.use.WarpMenu
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WarpMenuCommand : KoinComponent {
    private val localizationProvider: LocalizationProvider by inject()

    fun register(commands: Commands) {
        commands.register(
            "warpmenu",
            "Open the warp menu",
            emptyList(),
            object : BasicCommand {
                override fun permission(): String = "waystonewarps.command.warpmenu"

                override fun execute(commandSourceStack: io.papermc.paper.command.brigadier.CommandSourceStack, args: Array<String>) {
                    val sender = commandSourceStack.getSender()
                    val player = sender as? Player
                    if (player == null) {
                        sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED))
                        return
                    }

                    val menuNavigator = MenuNavigator(player)
                    WarpMenu(player, menuNavigator, localizationProvider).open()
                }
            }
        )
    }
}
