package com.herzchen.moreenchant.enchantments.virtualexplosion.manager

import com.herzchen.moreenchant.MoreEnchant

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import kotlin.math.min
import kotlin.random.Random

class VirtualExplosionManager(private val plugin: MoreEnchant) {
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

    private val experienceValues = mapOf(
        Material.COAL to { Random.Default.nextInt(0, 3) },
        Material.DIAMOND to { Random.Default.nextInt(3, 8) },
        Material.EMERALD to { Random.Default.nextInt(3, 8) },
        Material.LAPIS_LAZULI to { Random.Default.nextInt(2, 6) },
        Material.REDSTONE to { Random.Default.nextInt(1, 6) },
        Material.QUARTZ to { Random.Default.nextInt(2, 6) },
        Material.RAW_IRON to { Random.Default.nextInt(1, 3) },
        Material.RAW_GOLD to { Random.Default.nextInt(1, 3) },
        Material.RAW_COPPER to { Random.Default.nextInt(1, 3) }
    )

    private val materialColors = mapOf(
        Material.STONE to "§7",
        Material.COBBLESTONE to "§7",
        Material.COAL to "§0",
        Material.IRON_ORE to "§f",
        Material.GOLD_ORE to "§e",
        Material.DIAMOND to "§b",
        Material.EMERALD to "§a",
        Material.REDSTONE to "§c",
        Material.LAPIS_LAZULI to "§9",
        Material.QUARTZ to "§f",
        Material.RAW_IRON to "§f",
        Material.RAW_GOLD to "§e",
        Material.RAW_COPPER to "§6",
        Material.IRON_INGOT to "§f",
        Material.GOLD_INGOT to "§e",
        Material.COPPER_INGOT to "§6",
        Material.DEEPSLATE_COAL_ORE to "§8",
        Material.DEEPSLATE_IRON_ORE to "§f",
        Material.DEEPSLATE_GOLD_ORE to "§e",
        Material.DEEPSLATE_DIAMOND_ORE to "§b",
        Material.DEEPSLATE_EMERALD_ORE to "§a",
        Material.DEEPSLATE_REDSTONE_ORE to "§c",
        Material.DEEPSLATE_LAPIS_ORE to "§9"
    )

    private fun getMaterialDisplayName(material: Material): String {
        return when (material) {
            Material.STONE -> "Đá"
            Material.COBBLESTONE -> "Đá Cuội"
            Material.COAL -> "Than"
            Material.IRON_ORE -> "Quặng Sắt"
            Material.GOLD_ORE -> "Quặng Vàng"
            Material.DIAMOND -> "Kim Cương"
            Material.EMERALD -> "Ngọc Lục Bảo"
            Material.REDSTONE -> "Đá Đỏ"
            Material.LAPIS_LAZULI -> "Lưu Ly"
            Material.QUARTZ -> "Thạch Anh"
            Material.RAW_IRON -> "Sắt Thô"
            Material.RAW_GOLD -> "Vàng Thô"
            Material.RAW_COPPER -> "Đồng Thô"
            Material.IRON_INGOT -> "Thỏi Sắt"
            Material.GOLD_INGOT -> "Thỏi Vàng"
            Material.COPPER_INGOT -> "Thỏi Đồng"
            Material.DEEPSLATE_COAL_ORE -> "Quặng Than Đá Bảng Sâu"
            Material.DEEPSLATE_IRON_ORE -> "Quặng Sắt Đá Bảng Sâu"
            Material.DEEPSLATE_GOLD_ORE -> "Quặng Vàng Đá Bảng Sâu"
            Material.DEEPSLATE_DIAMOND_ORE -> "Quặng Kim Cương Đá Bảng Sâu"
            Material.DEEPSLATE_EMERALD_ORE -> "Quặng Ngọc Lục Bảo Đá Bảng Sâu"
            Material.DEEPSLATE_REDSTONE_ORE -> "Quặng Đá Đỏ Đá Bảng Sâu"
            Material.DEEPSLATE_LAPIS_ORE -> "Quặng Lưu Ly Đá Bảng Sâu"
            else -> material.name
        }
    }

    fun calculateExperience(material: Material): Int {
        return experienceValues[material]?.invoke() ?: 0
    }

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


        val dropRates = permissionManager.getBestDropGroup(player)
        val fortuneLevel = player.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)

        val shouldPause = shouldPauseExplosion(centerBlockLocation)

        plugin.bossBarManager.hideBossBar(player)

        if (shouldPause) {
            plugin.bossBarManager.showBossBar(player,
                "§cNổ ảo đã bị tạm dừng, vui lòng dọn dẹp vật phẩm xung quanh bạn!",
                org.bukkit.boss.BarColor.RED)
            return
        }

        val totalBlocks = min(shape.totalBlocks - 1, config.maxBlocks)
        val (items, totalExperience) = calculateDropsAndExperience(totalBlocks, dropRates, fortuneLevel)

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
                org.bukkit.boss.BarColor.YELLOW)

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val item = player.inventory.itemInMainHand
                val hasEnchant = plugin.enchantManager.getEnchantShape(item) != null

                if (hasEnchant) {
                    val isPaused = shouldPauseExplosion(player.location)
                    if (isPaused) {
                        plugin.bossBarManager.showBossBar(player,
                            "§cNổ ảo đã bị tạm dừng, vui lòng dọn dẹp vật phẩm xung quanh bạn!",
                            org.bukkit.boss.BarColor.RED)
                    } else {
                        plugin.bossBarManager.showBossBar(player,
                            "§aHiện đang kích hoạt Nổ Ảo",
                            org.bukkit.boss.BarColor.GREEN)
                    }
                } else {
                    plugin.bossBarManager.hideBossBar(player)
                }
            }, 20L)
        })
    }

    private fun calculateDropsAndExperience(
        blockCount: Int,
        dropRates: Map<Material, Double>,
        fortuneLevel: Int
    ): Pair<List<ItemStack>, Int> {
        if (dropRates.isEmpty()) return Pair(emptyList(), 0)

        val totalWeight = dropRates.values.sum()
        if (totalWeight <= 0) return Pair(emptyList(), 0)

        val items = mutableListOf<ItemStack>()
        var totalExperience = 0

        repeat(blockCount) {
            val random = Random.Default.nextDouble(0.0, totalWeight)
            var cumulative = 0.0

            for ((material, weight) in dropRates) {
                cumulative += weight
                if (random <= cumulative) {
                    val amount = calculateDropAmount(material, fortuneLevel)
                    items.add(ItemStack(material, amount))

                    totalExperience += calculateExperience(material)
                    break
                }
            }
        }
        return Pair(items, totalExperience)
    }

    private fun calculateDropAmount(material: Material, fortuneLevel: Int): Int {
        if (!fortuneAffectedMaterials.contains(material) || fortuneLevel <= 0) {
            return 1
        }

        return when (material) {
            Material.LAPIS_LAZULI -> {
                val baseAmount = Random.Default.nextInt(4, 10)
                applyFortuneMultiplier(baseAmount, fortuneLevel)
            }
            Material.REDSTONE -> {
                val baseAmount = Random.Default.nextInt(4, 6)
                applyFortuneMultiplier(baseAmount, fortuneLevel)
            }
            Material.RAW_COPPER -> {
                val baseAmount = Random.Default.nextInt(2, 6)
                applyFortuneMultiplier(baseAmount, fortuneLevel)
            }
            else -> {
                applyFortuneMultiplier(1, fortuneLevel)
            }
        }
    }

    private fun applyFortuneMultiplier(baseAmount: Int, fortuneLevel: Int): Int {
        val r = Random.Default.nextInt(0, fortuneLevel + 2)
        val multiplier = if (r <= 1) 1 else r
        return baseAmount * multiplier
    }

    private fun dropItems(items: List<ItemStack>, location: Location) {
        items.forEach { item ->
            location.world?.dropItemNaturally(location, item)
        }
    }

    fun shouldPauseExplosion(location: Location): Boolean {
        val maxItems = config.maxNearbyItems
        val radius = config.checkRadius

        if (maxItems <= 0) return false

        val nearbyItems =
            location.world?.getNearbyEntities(location, radius.toDouble(), radius.toDouble(), radius.toDouble())
                ?.count { it is Item } ?: 0

        return nearbyItems >= maxItems
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

            val color = materialColors[material] ?: "§f"
            val displayName = getMaterialDisplayName(material)

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
