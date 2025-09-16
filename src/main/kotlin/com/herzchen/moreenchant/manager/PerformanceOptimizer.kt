package com.herzchen.moreenchant.manager

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.Chunk
import org.bukkit.entity.Item
import org.bukkit.entity.Player

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class PerformanceOptimizer(private val plugin: MoreEnchant) {
    private val itemCountCache = ConcurrentHashMap<UUID, Int>()
    private val chunkItemCounts = ConcurrentHashMap<Chunk, AtomicInteger>()

    init {
        startScheduler()
    }

    private fun startScheduler() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            for (world in plugin.server.worlds) {
                for (chunk in world.loadedChunks) {
                    updateChunkItemCount(chunk)
                }
            }
        }, 0L, 200L)
    }

    private fun updateChunkItemCount(chunk: Chunk) {
        val count = chunkItemCounts.getOrPut(chunk) { AtomicInteger(0) }
        count.set(chunk.entities.count { it is Item })
    }

    fun getNearbyItemCount(player: Player, radius: Int = plugin.configManager.checkRadius): Int {
        val location = player.location
        val chunks = mutableSetOf<Chunk>()
        for (x in -radius..radius) for (z in -radius..radius) {
            val chunkX = location.chunk.x + x
            val chunkZ = location.chunk.z + z
            val chunk = if (location.world.isChunkLoaded(chunkX, chunkZ)) {
                location.world.getChunkAt(chunkX, chunkZ)
            } else continue
            chunks.add(chunk)
        }
        return chunks.sumOf { chunkItemCounts.getOrDefault(it, AtomicInteger(0)).get() }
    }

    fun getCachedItemCount(player: Player): Int {
        return itemCountCache.getOrDefault(player.uniqueId, getNearbyItemCount(player))
    }

    fun clearCache(playerId: UUID) {
        itemCountCache.remove(playerId)
    }
}