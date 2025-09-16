package com.herzchen.moreenchant.utils

import org.bukkit.Material

import kotlin.random.Random

object ExperienceUtils {
    fun calculateBlockExperience(material: Material): Int {
        return when (material) {
            Material.COAL_ORE -> Random.nextInt(0, 2)
            Material.DIAMOND_ORE -> Random.nextInt(3, 7)
            Material.EMERALD_ORE -> Random.nextInt(3, 7)
            Material.LAPIS_ORE -> Random.nextInt(2, 5)
            Material.NETHER_QUARTZ_ORE -> Random.nextInt(2, 5)
            Material.REDSTONE_ORE -> Random.nextInt(1, 5)
            else -> 0
        }
    }

    fun calculateOreExperience(material: Material): Int {
        return when (material) {
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> Random.nextInt(1, 3)
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> Random.nextInt(1, 3)
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> Random.nextInt(1, 3)
            Material.NETHER_GOLD_ORE -> Random.nextInt(1, 3)
            Material.ANCIENT_DEBRIS -> 2
            else -> 0
        }
    }
}