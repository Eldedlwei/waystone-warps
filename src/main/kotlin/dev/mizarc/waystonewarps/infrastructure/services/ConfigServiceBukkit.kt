package dev.mizarc.waystonewarps.infrastructure.services

import dev.mizarc.waystonewarps.application.services.ConfigService
import dev.mizarc.waystonewarps.infrastructure.services.teleportation.CostType
import org.bukkit.configuration.file.FileConfiguration
import java.util.Locale

class ConfigServiceBukkit(private val configFile: FileConfiguration): ConfigService {

    override fun getPluginLanguage(): String {
        val raw = getStringCompat(
            defaultValue = "en",
            "plugin_language",
            "plugin-language",
            "pluginLanguage",
            "language"
        )
        return raw.trim()
            .replace('-', '_')
            .lowercase(Locale.ROOT)
    }

    override fun getWarpLimit(): Int {
        return getIntCompat(
            defaultValue = 3,
            "warp_limit",
            "warp-limit",
            "warpLimit"
        )
    }

    override fun getTeleportTimer(): Int {
        return getIntCompat(
            defaultValue = 5,
            "teleport_timer",
            "teleport-timer",
            "teleportTimer"
        )
    }

    override fun getTeleportCostType(): CostType {
        return when (getStringCompat(
            defaultValue = "ITEM",
            "teleport_cost_type",
            "teleport-cost-type",
            "teleportCostType"
        ).uppercase(Locale.ROOT)) {
            "XP", "EXP", "EXPERIENCE" -> CostType.XP
            "MONEY", "VAULT", "ECONOMY" -> CostType.MONEY
            else -> CostType.ITEM
        }
    }

    override fun getTeleportCostItem(): String {
        return getStringCompat(
            defaultValue = "ENDER_PEARL",
            "teleport_cost_item",
            "teleport-cost-item",
            "teleportCostItem"
        )
    }

    override fun getTeleportCostAmount(): Double {
        return getDoubleCompat(
            defaultValue = 3.0,
            "teleport_cost_amount",
            "teleport-cost-amount",
            "teleportCostAmount"
        )
    }

    override fun getPlatformReplaceBlocks(): Set<String> {
        return getStringListCompat(
            "platform_replace_blocks",
            "platform-replace-blocks",
            "platformReplaceBlocks"
        ).toSet()
    }

    override fun getAllSkinTypes(): List<String> {
        val path = resolvePath("waystone_skins", "waystone-skins", "waystoneSkins", "warp_skins", "warpSkins")
            ?: return emptyList()
        return configFile.getConfigurationSection(path)?.getKeys(false)?.toList() ?: emptyList()
    }

    override fun getStructureBlocks(blockType: String): List<String> {
        val root = resolvePath("waystone_skins", "waystone-skins", "waystoneSkins", "warp_skins", "warpSkins")
            ?: return emptyList()
        val section = configFile.getConfigurationSection(root) ?: return emptyList()

        if (section.contains(blockType)) {
            return section.getStringList(blockType)
        }

        val caseInsensitiveKey = section.getKeys(false).firstOrNull { it.equals(blockType, ignoreCase = true) }
            ?: return emptyList()
        return section.getStringList(caseInsensitiveKey)
    }

    override fun allowWarpsMenuViaCompass(): Boolean {
        return getBooleanCompat(
            defaultValue = true,
            "warps_menu_via_compass",
            "warps-menu-via-compass",
            "warpsMenuViaCompass"
        )
    }

    override fun allowWarpsMenuViaWaystone(): Boolean {
        return getBooleanCompat(
            defaultValue = false,
            "warps_menu_via_waystone",
            "warps-menu-via-waystone",
            "warpsMenuViaWaystone",
            "warps_menu_via_lodestone",
            "warps-menu-via-lodestone",
            "warpsMenuViaLodestone"
        )
    }

    override fun hologramsEnabled(): Boolean {
        return getBooleanCompat(
            defaultValue = true,
            "holograms_enabled",
            "holograms-enabled",
            "hologramsEnabled"
        )
    }

    private fun resolvePath(vararg paths: String): String? {
        return paths.firstOrNull { configFile.contains(it) }
    }

    private fun getStringCompat(defaultValue: String, vararg paths: String): String {
        val path = resolvePath(*paths) ?: return defaultValue
        return configFile.getString(path, defaultValue) ?: defaultValue
    }

    private fun getIntCompat(defaultValue: Int, vararg paths: String): Int {
        val path = resolvePath(*paths) ?: return defaultValue
        return configFile.getInt(path, defaultValue)
    }

    private fun getDoubleCompat(defaultValue: Double, vararg paths: String): Double {
        val path = resolvePath(*paths) ?: return defaultValue
        return configFile.getDouble(path, defaultValue)
    }

    private fun getBooleanCompat(defaultValue: Boolean, vararg paths: String): Boolean {
        val path = resolvePath(*paths) ?: return defaultValue
        return configFile.getBoolean(path, defaultValue)
    }

    private fun getStringListCompat(vararg paths: String): List<String> {
        val path = resolvePath(*paths) ?: return emptyList()
        return configFile.getStringList(path)
    }
}
