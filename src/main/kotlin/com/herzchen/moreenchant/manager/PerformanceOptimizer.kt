package com.herzchen.moreenchant.manager

import com.herzchen.moreenchant.MoreEnchant
import org.bukkit.entity.Item
import org.bukkit.entity.Player

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PerformanceOptimizer(private val plugin: MoreEnchant) {
    private val itemCountCache = ConcurrentHashMap<UUID, Int>()

    init {
        startScheduler()
    }

    private fun startScheduler() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            for (player in plugin.server.onlinePlayers) {
                val count = calculateNearbyItems(player)
                itemCountCache[player.uniqueId] = count
            }
        }, 0L, 20L)
    }

    private fun calculateNearbyItems(player: Player): Int {
        val location = player.location
        val radius = plugin.configManager.checkRadius.toDouble()
        return location.world?.getNearbyEntities(location, radius, radius, radius)
            ?.count { it is Item } ?: 0
    }

    fun getCachedItemCount(player: Player): Int {
        return itemCountCache.getOrDefault(player.uniqueId, 0)
    }

    fun clearCache(playerId: UUID) {
        itemCountCache.remove(playerId)
    }
}