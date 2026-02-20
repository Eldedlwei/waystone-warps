package dev.mizarc.waystonewarps.interaction.commands

import dev.mizarc.waystonewarps.application.actions.world.CreateWarp
import dev.mizarc.waystonewarps.application.results.CreateWarpResult
import dev.mizarc.waystonewarps.domain.positioning.Position3D
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WarpCreateCommand : KoinComponent {
    private val createWarp: CreateWarp by inject()
    private val localization: LocalizationProvider by inject()

    fun register(commands: Commands) {
        commands.register(
            "warpcreate",
            "Create a new warp with the Lodestone you're looking at",
            emptyList(),
            object : BasicCommand {
                override fun permission(): String = "waystonewarps.create"

                override fun execute(commandSourceStack: CommandSourceStack, args: Array<String>) {
                    val sender = commandSourceStack.getSender()
                    val player = sender as? Player
                    if (player == null) {
                        sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED))
                        return
                    }

                    if (args.isEmpty()) {
                        player.sendMessage(Component.text("Usage: /warpcreate <name>", NamedTextColor.RED))
                        return
                    }

                    onWarpCreate(player, args.joinToString(" ").trim())
                }
            }
        )
    }

    private fun onWarpCreate(player: Player, name: String) {
        val playerId = player.uniqueId

        val targetBlock = player.getTargetBlockExact(10)
        if (targetBlock == null) {
            player.sendMessage(localization.get(playerId, "feedback.create.not_within_range"))
            return
        }

        if (targetBlock.type != Material.LODESTONE) {
            player.sendMessage(localization.get(playerId, "feedback.create.not_lodestone"))
            return
        }

        if (targetBlock.getRelative(org.bukkit.block.BlockFace.DOWN).type != Material.SMOOTH_STONE) {
            player.sendMessage(localization.get(playerId, "feedback.create.not_smooth_stone"))
            return
        }

        val blockLoc = targetBlock.location
        val position = Position3D(
            x = blockLoc.x.toInt(),
            y = blockLoc.y.toInt(),
            z = blockLoc.z.toInt(),
        )
        val worldId = blockLoc.world?.uid ?: run {
            player.sendMessage(localization.get(playerId, "feedback.create.world_not_found"))
            return
        }

        val result = createWarp.execute(
            playerId = playerId,
            name = name,
            position3D = position,
            worldId = worldId,
            baseBlock = "LODESTONE"
        )

        when (result) {
            is CreateWarpResult.Success -> {
                player.sendMessage(localization.get(playerId, "feedback.create.success"))
            }

            CreateWarpResult.LimitExceeded -> {
                player.sendMessage(localization.get(playerId, "condition.naming.limit"))
            }

            CreateWarpResult.NameCannotBeBlank -> {
                player.sendMessage(localization.get(playerId, "condition.naming.blank"))
            }

            CreateWarpResult.NameAlreadyExists -> {
                player.sendMessage(localization.get(playerId, "condition.naming.existing"))
            }
        }
    }
}
