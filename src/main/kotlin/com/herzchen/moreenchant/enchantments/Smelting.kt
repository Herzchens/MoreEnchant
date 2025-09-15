package com.herzchen.moreenchant.enchantments

import com.herzchen.moreenchant.MoreEnchant
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import kotlin.random.Random

class Smelting(private val plugin: MoreEnchant) {
    private val vanillaSmeltingMap = mapOf(
        Material.IRON_ORE to Material.IRON_INGOT,
        Material.DEEPSLATE_IRON_ORE to Material.IRON_INGOT,
        Material.GOLD_ORE to Material.GOLD_INGOT,
        Material.DEEPSLATE_GOLD_ORE to Material.GOLD_INGOT,
        Material.COPPER_ORE to Material.COPPER_INGOT,
        Material.DEEPSLATE_COPPER_ORE to Material.COPPER_INGOT,
        Material.NETHER_GOLD_ORE to Material.GOLD_INGOT,
        Material.ANCIENT_DEBRIS to Material.NETHERITE_SCRAP
    )

    private val baseAmounts = mapOf(
        Material.COPPER_ORE to { Random.nextInt(2, 6) },
        Material.DEEPSLATE_COPPER_ORE to { Random.nextInt(2, 6) }
    )

    private val materialColors = mapOf(
        Material.IRON_INGOT to "§f",
        Material.GOLD_INGOT to "§e",
        Material.COPPER_INGOT to "§6",
        Material.NETHERITE_SCRAP to "§4",
    )

    private val materialNames = mapOf(
        Material.IRON_INGOT to "Thỏi Sắt",
        Material.GOLD_INGOT to "Thỏi Vàng",
        Material.COPPER_INGOT to "Thỏi Đồng",
        Material.NETHERITE_SCRAP to "Mảnh Netherite",
    )

    private val levelConfig = mutableMapOf<String, SmeltingLevel>()
    private val customBlocks = mutableMapOf<Material, CustomBlock>()
    private val enabledVanillaBlocks = mutableSetOf<Material>()

    data class SmeltingLevel(val chance: Double, val cooldown: Double)
    data class CustomBlock(val rawItem: Material, val smeltedItem: Material, val fortuneApplies: Boolean)

    init {
        loadConfig()
    }

    fun loadConfig() {
        val configFile = File(plugin.dataFolder, "enchantments/smelting.yml").apply {
            parentFile.mkdirs()
            if (!exists()) plugin.saveResource("enchantments/smelting.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(configFile)

        levelConfig.clear()
        val levelsSection = config.getConfigurationSection("levels") ?: return
        for (key in levelsSection.getKeys(false)) {
            val chance = levelsSection.getDouble("$key.chance")
            val cooldown = levelsSection.getDouble("$key.cooldown")
            levelConfig[key] = SmeltingLevel(chance, cooldown)
        }

        enabledVanillaBlocks.clear()
        val vanillaSection = config.getConfigurationSection("vanilla_blocks") ?: return
        for (materialName in vanillaSection.getKeys(false)) {
            if (vanillaSection.getBoolean(materialName)) {
                Material.matchMaterial(materialName)?.let { enabledVanillaBlocks.add(it) }
            }
        }

        customBlocks.clear()
        val customSection = config.getConfigurationSection("custom_blocks") ?: return
        for (key in customSection.getKeys(false)) {
            val rawItemName = customSection.getString("$key.raw_item") ?: continue
            val smeltedItemName = customSection.getString("$key.smelted_item") ?: continue
            val fortuneApplies = customSection.getBoolean("$key.fortune_applies", true)

            val rawItem = Material.matchMaterial(rawItemName)
            val smeltedItem = Material.matchMaterial(smeltedItemName)

            if (rawItem != null && smeltedItem != null) {
                customBlocks[rawItem] = CustomBlock(rawItem, smeltedItem, fortuneApplies)
            }
        }
    }

    fun getSmeltingResult(original: Material, fortuneLevel: Int = 0): Pair<Material, Int>? {
        if (enabledVanillaBlocks.contains(original)) {
            val result = vanillaSmeltingMap[original] ?: return null

            val baseAmount = if (original == Material.ANCIENT_DEBRIS) {
                1
            } else {
                baseAmounts[original]?.invoke() ?: 1
            }

            val finalAmount = if (fortuneLevel > 0 && original != Material.ANCIENT_DEBRIS && original != Material.NETHER_GOLD_ORE) {
                applyFortune(baseAmount, fortuneLevel)
            } else {
                baseAmount
            }

            return Pair(result, finalAmount)
        }

        val customBlock = customBlocks[original]
        if (customBlock != null) {
            val amount = if (customBlock.fortuneApplies && fortuneLevel > 0) {
                applyFortune(1, fortuneLevel)
            } else {
                1
            }
            return Pair(customBlock.smeltedItem, amount)
        }

        return null
    }

    private fun applyFortune(baseAmount: Int, fortuneLevel: Int): Int {
        return when (fortuneLevel) {
            1 -> baseAmount * (if (Random.nextDouble() < 0.33) 2 else 1)
            2 -> baseAmount * when ((Random.nextDouble() * 100).toInt()) {
                in 0..24 -> 1
                in 25..49 -> 2
                else -> 3
            }
            3 -> baseAmount * when ((Random.nextDouble() * 100).toInt()) {
                in 0..19 -> 1
                in 20..39 -> 2
                in 40..59 -> 3
                else -> 4
            }
            else -> baseAmount
        }
    }

    fun getLevelConfig(level: String): SmeltingLevel? {
        return levelConfig[level]
    }

    fun getAllLevels(): Set<String> {
        return levelConfig.keys
    }

    fun shouldSmelt(item: ItemStack, level: String): Boolean {
        if (item.containsEnchantment(Enchantment.SILK_TOUCH)) return false

        val config = getLevelConfig(level) ?: return false
        return Random.nextDouble() * 100 < config.chance
    }

    fun calculateSmeltingExperience(material: Material): Int {
        return when (material) {
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> Random.nextInt(1, 3)
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> Random.nextInt(1, 3)
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> Random.nextInt(1, 3)
            Material.NETHER_GOLD_ORE -> Random.nextInt(1, 3)
            Material.ANCIENT_DEBRIS -> 2
            else -> 0
        }
    }

    fun showSmeltingResultMessage(player: Player, material: Material, amount: Int, exp: Int) {
        val color = materialColors[material] ?: "§f"
        val displayName = materialNames[material] ?: material.name

        val message = "§f+ $amount $color$displayName" + if (exp > 0) " §7(+$exp EXP)" else ""
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message))
    }
}