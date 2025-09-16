package com.herzchen.moreenchant.manager

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.entity.Player

class PermissionManager(private val configManager: ConfigManager) {
    constructor(plugin: MoreEnchant) : this(plugin.configManager)

    fun getBestDropGroup(player: Player): ConfigManager.DropGroup? {
        return configManager.dropGroups.values
            .firstOrNull { it.permission.isEmpty() || player.hasPermission(it.permission) }
            ?: configManager.dropGroups["default"]
    }
}