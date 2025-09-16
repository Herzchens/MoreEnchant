package com.herzchen.moreenchant.listener

import com.github.benmanes.caffeine.cache.Caffeine

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
import java.util.concurrent.*

import kotlin.random.Random

class BlockBreakListener(private val plugin: MoreEnchant) : Listener {
    private val enchantManager = plugin.enchantManager

    private val cooldowns = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<Pair<UUID, String>, Long>()

    private val transactionQueues = ConcurrentHashMap<UUID, LinkedBlockingQueue<() -> Unit>>()
    private val executor = Executors.newCachedThreadPool()

    init {
        startTransactionProcessor()
    }

    private fun startTransactionProcessor() {
        executor.submit {
            while (!Thread.currentThread().isInterrupted) {
                transactionQueues.forEach { (uuid, queue) ->
                    try {
                        val operation = queue.poll(100, TimeUnit.MILLISECONDS)
                        operation?.invoke()
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@submit
                    } catch (e: Exception) {
                        plugin.logger.warning("Lỗi khi xử lý storage operation cho player $uuid: ${e.message}")
                    }
                }
            }
        }
    }

    fun enqueueStorageOperation(player: Player, operation: () -> Unit) {
        val queue = transactionQueues.getOrPut(player.uniqueId) {
            LinkedBlockingQueue()
        }
        try {
            queue.put(operation)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        cooldowns.asMap().keys.removeAll { it.first == playerId }
        plugin.performanceOptimizer.clearCache(playerId)
        transactionQueues.remove(playerId)

        plugin.bossBarManager.removePlayerBossBar(event.player)
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

                        enqueueStorageOperation(player) {
                            plugin.server.scheduler.runTask(plugin, Runnable {
                                val (successfulItems, failedItems) = plugin.extraStorageHook.addToStorage(player, listOf(smeltedItem))

                                if (successfulItems.isNotEmpty()) {
                                    plugin.smelting!!.showSmeltingResultMessage(player, resultMaterial, amount, exp)
                                }

                                if (failedItems.isNotEmpty()) {
                                    failedItems.forEach { drop ->
                                        block.world.dropItemNaturally(block.location, drop)
                                    }
                                }
                            })
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

        if (!checkCooldown(player, shapeKey)) {
            return
        }

        event.isCancelled = true

        if (hasAutoPickup) {
            val originalDrops = block.getDrops(item).toList()
            val originalExp = calculateBlockExperience(block)

            enqueueStorageOperation(player) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (originalDrops.isNotEmpty()) {
                        val (_, failedItems) = plugin.extraStorageHook.addToStorage(player, originalDrops)
                        if (failedItems.isNotEmpty()) {
                            failedItems.forEach { drop ->
                                block.world.dropItemNaturally(block.location, drop)
                            }
                        }
                    }

                    if (originalExp > 0) {
                        player.giveExp(originalExp)
                    }
                })
            }

            block.type = Material.AIR
        } else {
            block.breakNaturally(item)
        }

        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                plugin.virtualExplosion!!.handleVirtualExplosion(player, block.location, shapeKey)
            } catch (e: Exception) {
                plugin.logger.warning("Lỗi khi xử lý vụ nổ ảo: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    private fun checkCooldown(player: Player, shapeKey: String): Boolean {
        val cooldownKey = player.uniqueId to shapeKey
        val lastUse = cooldowns.getIfPresent(cooldownKey)
        val currentTime = System.currentTimeMillis()
        val shape = plugin.configManager.getExplosionShape(shapeKey) ?: return false
        val cooldownMs = (shape.cooldown * 1000).toLong()

        return if (lastUse != null && currentTime - lastUse < cooldownMs) {
            false
        } else {
            cooldowns.put(cooldownKey, currentTime)
            true
        }
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

    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}