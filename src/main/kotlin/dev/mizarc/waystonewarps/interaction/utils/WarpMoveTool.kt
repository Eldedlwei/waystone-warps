package dev.mizarc.waystonewarps.interaction.utils

import dev.mizarc.waystonewarps.domain.warps.Warp
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun getWarpMoveTool(warp: Warp): ItemStack {
    return ItemStack(Material.LODESTONE)
        .name(Component.text("Move Warp '${warp.name}'").color(NamedTextColor.AQUA))
        .lore("Place this where you want the new location to be.")
        .setStringMeta("warp", warp.id.toString())
}
