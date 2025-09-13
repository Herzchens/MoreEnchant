package com.herzchen.moreenchant.manager

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.Material
import org.bukkit.entity.Player

class PermissionManager(private val configManager: ConfigManager) {
    constructor(plugin: MoreEnchant) : this(plugin.configManager)

    fun getBestDropGroup(player: Player): Map<Material, Double> {
        return configManager.dropGroups.values
            .firstOrNull { it.permission.isEmpty() || player.hasPermission(it.permission) }
            ?.drops
            ?: configManager.dropGroups["default"]?.drops
            ?: emptyMap()
    }
}