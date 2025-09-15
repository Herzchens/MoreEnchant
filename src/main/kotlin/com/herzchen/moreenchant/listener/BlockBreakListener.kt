package com.herzchen.moreenchant.listener

import com.herzchen.moreenchant.MoreEnchant
import com.herzchen.moreenchant.utils.ExperienceUtils

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

import java.util.*
import java.util.concurrent.ConcurrentHashMap

import kotlin.random.Random

class BlockBreakListener(private val plugin: MoreEnchant) : Listener {
    private val enchantManager = plugin.enchantManager
    private val cooldowns = ConcurrentHashMap<Pair<UUID, String>, Long>()

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        cooldowns.keys.removeAll { it.first == playerId }
        plugin.performanceOptimizer.clearCache(playerId)
    }

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

        val hasSilkTouch = item.containsEnchantment(Enchantment.SILK_TOUCH)
        val smeltingLevel = enchantManager.getSmeltingLevel(item)

        if (!hasSilkTouch && smeltingLevel != null && plugin.smelting != null) {
            if (plugin.smelting!!.shouldSmelt(item, smeltingLevel)) {
                val fortuneLevel = if (block.type == Material.ANCIENT_DEBRIS || block.type == Material.NETHER_GOLD_ORE) {
                    0
                } else {
                    item.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)
                }
                val smeltingResult = plugin.smelting!!.getSmeltingResult(block.type, fortuneLevel)
                if (smeltingResult != null) {
                    event.isCancelled = true
                    val (resultMaterial, amount) = smeltingResult
                    val exp = plugin.smelting!!.calculateSmeltingExperience(block.type)
                    if (hasAutoPickup) {
                        val smeltedItem = ItemStack(resultMaterial, amount)
                        val (successfulItems, failedItems) = plugin.extraStorageHook.addToStorage(player, listOf(smeltedItem))

                        if (successfulItems.isNotEmpty()) {
                            plugin.smelting!!.showSmeltingResultMessage(player, resultMaterial, amount, exp)
                        }

                        if (failedItems.isNotEmpty()) {
                            failedItems.forEach { drop ->
                                block.world.dropItemNaturally(block.location, drop)
                            }
                        }
                    } else {
                        block.world.dropItemNaturally(block.location, ItemStack(resultMaterial, amount))
                        plugin.smelting!!.showSmeltingResultMessage(player, resultMaterial, amount, exp)
                    }
                    if (exp > 0) {
                        player.giveExp(exp)
                    }
                    block.type = Material.AIR
                    return
                }
            }
        }

        if (!plugin.configManager.blockWhitelist.contains(block.type)) {
            return
        }

        if (plugin.virtualExplosion == null) {
            return
        }

        if (plugin.virtualExplosion!!.shouldPauseExplosion(player)) {
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

        event.isCancelled = true

        if (hasAutoPickup) {
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
                plugin.virtualExplosion!!.handleVirtualExplosion(player, block.location, shapeKey)
            } catch (e: Exception) {
                plugin.logger.warning("Lỗi khi xử lý vụ nổ ảo: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    private fun calculateBlockExperience(block: Block): Int {
        return ExperienceUtils.calculateBlockExperience(block.type)
    }

    private fun canPlayerBreakBlocks(player: Player): Boolean {
        if (!plugin.extraStorageHook.isAvailable()) return true

        val hasAutoPickup = plugin.extraStorageHook.hasAutoPickup(player)
        if (!hasAutoPickup) return true

        return !plugin.extraStorageHook.isStorageFull(player)
    }
}