package com.herzchen.moreenchant.enchantments

import com.herzchen.moreenchant.MoreEnchant
import com.herzchen.moreenchant.manager.ConfigManager
import com.herzchen.moreenchant.utils.ExperienceUtils
import com.herzchen.moreenchant.utils.FortuneUtils
import com.herzchen.moreenchant.utils.MaterialUtils

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.boss.BarColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.collections.iterator

import kotlin.math.min
import kotlin.random.Random

class VirtualExplosion(private val plugin: MoreEnchant) {
    private val config = plugin.configManager
    private val permissionManager = plugin.permissionManager
    private val extraStorageHook = plugin.extraStorageHook

    private val fortuneAffectedMaterials = setOf(
        Material.COAL,
        Material.DIAMOND,
        Material.EMERALD,
        Material.LAPIS_LAZULI,
        Material.REDSTONE,
        Material.QUARTZ,
        Material.RAW_IRON,
        Material.RAW_GOLD,
        Material.RAW_COPPER
    )

    fun handleVirtualExplosion(
        player: Player,
        centerBlockLocation: Location,
        shapeKey: String
    ) {
        val shape = config.getExplosionShape(shapeKey) ?: return

        val hasAutoPickup = extraStorageHook.isAvailable() && extraStorageHook.hasAutoPickup(player)
        val isStorageFull = hasAutoPickup && extraStorageHook.isStorageFull(player)

        if (isStorageFull) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§cKho đã đầy! Tạm dừng nổ ảo."))
            return
        }
        if (shouldPauseDueToStorage(player)) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§cKho đã đầy! Tạm dừng nổ ảo."))
            return
        }

        val dropGroup = permissionManager.getBestDropGroup(player) ?: return
        val fortuneLevel = player.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)

        val shouldPause = shouldPauseExplosion(player)

        plugin.bossBarManager.hideBossBar(player)

        if (shouldPause) {
            plugin.bossBarManager.showBossBar(player,
                "§cNổ ảo đã bị tạm dừng, vui lòng dọn dẹp vật phẩm xung quanh bạn!",
                BarColor.RED)
            return
        }

        val totalBlocks = min(shape.totalBlocks - 1, config.maxBlocks)

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val (items, totalExperience) = calculateDropsAndExperience(totalBlocks, dropGroup, fortuneLevel)

            plugin.server.scheduler.runTask(plugin, Runnable {
                val useStorage = extraStorageHook.isAvailable() && extraStorageHook.hasAutoPickup(player)

                if (useStorage) {
                    val (successfulItems, failedItems) = addToExtraStorage(items, player)
                    if (successfulItems.isNotEmpty()) {
                        showAddedItemsMessage(player, successfulItems)
                    }
                    if (failedItems.isNotEmpty()) {
                        dropItems(failedItems, centerBlockLocation)
                    }
                } else {
                    dropItems(items, centerBlockLocation)
                }

                if (totalExperience > 0) {
                    player.giveExp(totalExperience)
                }

                plugin.bossBarManager.showBossBar(player,
                    "§cNổ Ảo§7: §f${shapeKey} §7(§f+${totalExperience} §aEXP§7)",
                    BarColor.YELLOW)

                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val item = player.inventory.itemInMainHand
                    val hasEnchant = plugin.enchantManager.getEnchantShape(item) != null

                    if (hasEnchant) {
                        val isPaused = shouldPauseExplosion(player)
                        if (isPaused) {
                            plugin.bossBarManager.showBossBar(player,
                                "§cNổ ảo đã bị tạm dừng, vui lòng dọn dẹp vật phẩm xung quanh bạn!",
                                BarColor.RED)
                        } else {
                            plugin.bossBarManager.showBossBar(player,
                                "§aHiện đang kích hoạt Nổ Ảo",
                                BarColor.GREEN)
                        }
                    } else {
                        plugin.bossBarManager.hideBossBar(player)
                    }
                }, 20L)
            })
        })
    }

    private fun calculateDropsAndExperience(
        blockCount: Int,
        dropGroup: ConfigManager.DropGroup,
        fortuneLevel: Int
    ): Pair<List<ItemStack>, Int> {
        val cumulativeWeights = dropGroup.cumulativeWeights
        if (cumulativeWeights.isEmpty()) return Pair(emptyList(), 0)

        val totalWeight = cumulativeWeights.last().second
        if (totalWeight <= 0) return Pair(emptyList(), 0)

        val items = mutableListOf<ItemStack>()
        var totalExperience = 0

        repeat(blockCount) {
            val random = Random.Default.nextDouble(0.0, totalWeight)
            val material = findMaterial(cumulativeWeights, random)
            val amount = calculateDropAmount(material, fortuneLevel)
            items.add(ItemStack(material, amount))
            totalExperience += calculateExperience(material)
        }
        return Pair(items, totalExperience)
    }

    private fun findMaterial(cumulativeWeights: List<Pair<Material, Double>>, random: Double): Material {
        var low = 0
        var high = cumulativeWeights.size - 1
        while (low < high) {
            val mid = (low + high) / 2
            if (random < cumulativeWeights[mid].second) {
                high = mid
            } else {
                low = mid + 1
            }
        }
        return cumulativeWeights[low].first
    }

    private fun calculateDropAmount(material: Material, fortuneLevel: Int): Int {
        if (!fortuneAffectedMaterials.contains(material) || fortuneLevel <= 0) {
            return 1
        }

        return when (material) {
            Material.LAPIS_LAZULI -> {
                val baseAmount = Random.Default.nextInt(4, 10)
                FortuneUtils.applyFortune(baseAmount, fortuneLevel)
            }
            Material.REDSTONE -> {
                val baseAmount = Random.Default.nextInt(4, 6)
                FortuneUtils.applyFortune(baseAmount, fortuneLevel)
            }
            Material.RAW_COPPER -> {
                val baseAmount = Random.Default.nextInt(2, 6)
                FortuneUtils.applyFortune(baseAmount, fortuneLevel)
            }
            else -> {
                FortuneUtils.applyFortune(1, fortuneLevel)
            }
        }
    }

    private fun calculateExperience(material: Material): Int {
        // Sử dụng ExperienceUtils để tính toán kinh nghiệm
        return ExperienceUtils.calculateBlockExperience(material) + ExperienceUtils.calculateOreExperience(material)
    }

    private fun dropItems(items: List<ItemStack>, location: Location) {
        items.forEach { item ->
            location.world?.dropItemNaturally(location, item)
        }
    }

    fun shouldPauseExplosion(player: Player): Boolean {
        val maxItems = config.maxNearbyItems
        if (maxItems <= 0) return false
        val count = plugin.performanceOptimizer.getCachedItemCount(player)
        return count >= maxItems
    }

    private fun addToExtraStorage(items: List<ItemStack>, player: Player): Pair<List<ItemStack>, List<ItemStack>> {
        return if (extraStorageHook.isAvailable()) {
            extraStorageHook.addToStorage(player, items)
        } else {
            Pair(emptyList(), items)
        }
    }

    private fun showAddedItemsMessage(player: Player, items: List<ItemStack>) {
        val itemCounts = mutableMapOf<Material, Int>()

        for (item in items) {
            itemCounts[item.type] = itemCounts.getOrDefault(item.type, 0) + item.amount
        }

        val message = StringBuilder()
        var first = true

        for ((material, count) in itemCounts) {
            if (!first) {
                message.append("§7, ")
            }

            val color = MaterialUtils.getMaterialColor(material)
            val displayName = MaterialUtils.getMaterialDisplayName(material)

            message.append("§f+ §3$count $color$displayName")
            first = false
        }

        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message.toString()))
    }

    fun shouldPauseDueToStorage(player: Player): Boolean {
        if (!extraStorageHook.isAvailable()) return false

        val hasAutoPickup = extraStorageHook.hasAutoPickup(player)
        if (!hasAutoPickup) return false

        return extraStorageHook.isStorageFull(player)
    }

}