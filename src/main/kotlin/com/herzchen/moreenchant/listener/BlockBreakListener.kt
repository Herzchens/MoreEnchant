package com.herzchen.moreenchant.listener

import com.herzchen.moreenchant.MoreEnchant

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.Flags

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

import java.util.*

import kotlin.random.Random

class BlockBreakListener(private val plugin: MoreEnchant) : Listener {
    private val enchantManager = plugin.enchantManager
    private val explosionManager = plugin.virtualExplosionManager
    private val cooldowns = mutableMapOf<Pair<UUID, String>, Long>()

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val item = player.inventory.itemInMainHand

        if (player.gameMode == GameMode.CREATIVE) return

        val hasAutoPickup = plugin.extraStorageHook.isAvailable() &&
                plugin.extraStorageHook.hasAutoPickup(player)
        val isStorageFull = hasAutoPickup && plugin.extraStorageHook.isStorageFull(player)

        if (isStorageFull) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§cKho đã đầy! Không thể đập thêm khối."))
            event.isCancelled = true
            return
        }
        if (!canPlayerBreakBlocks(player)) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§cKho đã đầy! Không thể đập thêm khối."))
            event.isCancelled = true
            return
        }

        val shouldPreventExplosion = plugin.extraStorageHook.isAvailable() &&
                plugin.extraStorageHook.hasAutoPickup(player) &&
                plugin.extraStorageHook.isStorageFull(player)

        if (shouldPreventExplosion) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§cKho đã đầy! Tạm dừng nổ ảo."))

            event.isCancelled = true
            return
        }

        if (!plugin.configManager.blockWhitelist.contains(block.type)) {
            return
        }

        if (plugin.virtualExplosionManager.shouldPauseExplosion(block.location)) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§cTạm dừng nổ ảo: Quá nhiều vật phẩm xung quanh!"))
            return
        }

        val shapeKey = enchantManager.getEnchantShape(item) ?: return
        val shape = plugin.configManager.getExplosionShape(shapeKey) ?: return

        if (Random.nextDouble() * 100 >= shape.chance) {
            return
        }

        val cooldownKey = player.uniqueId to shapeKey
        val lastUse = cooldowns[cooldownKey]
        val currentTime = System.currentTimeMillis()
        val cooldownMs = (shape.cooldown * 1000).toLong()

        if (lastUse != null && currentTime - lastUse < cooldownMs) {
            return
        }

        if (!checkWorldGuard(player, block)) {
            return
        }

        event.isCancelled = true

        if (plugin.extraStorageHook.isAvailable() && plugin.extraStorageHook.hasAutoPickup(player)) {
            val originalDrops = block.getDrops(item).toList()
            if (originalDrops.isNotEmpty()) {
                val (_, failedItems) = plugin.extraStorageHook.addToStorage(player, originalDrops)
                if (failedItems.isNotEmpty()) {
                    failedItems.forEach { drop ->
                        block.world.dropItemNaturally(block.location, drop)
                    }
                }
            }

            val originalExp = calculateBlockExperience(block)
            if (originalExp > 0) {
                player.giveExp(originalExp)
            }

            block.type = Material.AIR
        } else {
            block.breakNaturally(item)
        }

        cooldowns[cooldownKey] = currentTime

        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                explosionManager.handleVirtualExplosion(player, block.location, shapeKey)
            } catch (e: Exception) {
                plugin.logger.warning("Lỗi khi xử lý vụ nổ ảo: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    private fun checkWorldGuard(player: Player, block: Block): Boolean {
        val wgPlugin = plugin.server.pluginManager.getPlugin("WorldGuard") ?: return true
        if (!wgPlugin.isEnabled) return true

        return try {
            val container = WorldGuard.getInstance().platform.regionContainer
            val query = container.createQuery()
            val wgLocation = BukkitAdapter.adapt(block.location)
            val localPlayer = WorldGuardPlugin.inst().wrapPlayer(player)
            query.testState(wgLocation, localPlayer, Flags.BUILD)
        } catch (ex: Exception) {
            plugin.logger.warning("Lỗi kiểm tra WorldGuard: ${ex.message}")
            true
        }
    }

    private fun calculateBlockExperience(block: Block): Int {
        return when (block.type) {
            Material.COAL_ORE -> Random.Default.nextInt(0, 2)
            Material.DIAMOND_ORE -> Random.Default.nextInt(3, 7)
            Material.EMERALD_ORE -> Random.Default.nextInt(3, 7)
            Material.LAPIS_ORE -> Random.Default.nextInt(2, 5)
            Material.NETHER_QUARTZ_ORE -> Random.Default.nextInt(2, 5)
            Material.REDSTONE_ORE -> Random.Default.nextInt(1, 5)
            else -> 0
        }
    }


    private fun canPlayerBreakBlocks(player: Player): Boolean {
        if (!plugin.extraStorageHook.isAvailable()) return true

        val hasAutoPickup = plugin.extraStorageHook.hasAutoPickup(player)
        if (!hasAutoPickup) return true

        return !plugin.extraStorageHook.isStorageFull(player)
    }
}