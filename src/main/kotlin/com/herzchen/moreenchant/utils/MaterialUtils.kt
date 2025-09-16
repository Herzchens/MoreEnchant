package com.herzchen.moreenchant.utils

import org.bukkit.Material

import java.util.concurrent.ConcurrentHashMap

object MaterialUtils {
    private val materialColorCache = ConcurrentHashMap<Material, String>()
    private val materialNameCache = ConcurrentHashMap<Material, String>()

    val materialColors = mapOf(
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
        Material.NETHERITE_SCRAP to "§4",

        Material.DEEPSLATE_COAL_ORE to "§8",
        Material.DEEPSLATE_IRON_ORE to "§f",
        Material.DEEPSLATE_GOLD_ORE to "§e",
        Material.DEEPSLATE_DIAMOND_ORE to "§b",
        Material.DEEPSLATE_EMERALD_ORE to "§a",
        Material.DEEPSLATE_REDSTONE_ORE to "§c",
        Material.DEEPSLATE_LAPIS_ORE to "§9",

        Material.COPPER_ORE to "§6",
        Material.DEEPSLATE_COPPER_ORE to "§6",
        Material.NETHER_GOLD_ORE to "§e",
        Material.ANCIENT_DEBRIS to "§4"
    )

    val materialNames = mapOf(
        Material.STONE to "Đá",
        Material.COBBLESTONE to "Đá Cuội",

        Material.COAL to "Than",

        Material.IRON_ORE to "Quặng Sắt",
        Material.GOLD_ORE to "Quặng Vàng",
        Material.DIAMOND to "Kim Cương",
        Material.EMERALD to "Ngọc Lục Bảo",
        Material.REDSTONE to "Đá Đỏ",
        Material.LAPIS_LAZULI to "Lưu Ly",
        Material.QUARTZ to "Thạch Anh",

        Material.RAW_IRON to "Sắt Thô",
        Material.RAW_GOLD to "Vàng Thô",
        Material.RAW_COPPER to "Đồng Thô",

        Material.IRON_INGOT to "Thỏi Sắt",
        Material.GOLD_INGOT to "Thỏi Vàng",
        Material.COPPER_INGOT to "Thỏi Đồng",
        Material.NETHERITE_SCRAP to "Mảnh Netherite",

        Material.DEEPSLATE_COAL_ORE to "Quặng Than Đá Bảng Sâu",
        Material.DEEPSLATE_IRON_ORE to "Quặng Sắt Đá Bảng Sâu",
        Material.DEEPSLATE_GOLD_ORE to "Quặng Vàng Đá Bảng Sâu",
        Material.DEEPSLATE_DIAMOND_ORE to "Quặng Kim Cương Đá Bảng Sâu",
        Material.DEEPSLATE_EMERALD_ORE to "Quặng Ngọc Lục Bảo Đá Bảng Sâu",
        Material.DEEPSLATE_REDSTONE_ORE to "Quặng Đá Đỏ Đá Bảng Sâu",
        Material.DEEPSLATE_LAPIS_ORE to "Quặng Lưu Ly Đá Bảng Sâu",

        Material.COPPER_ORE to "Quặng Đồng",
        Material.DEEPSLATE_COPPER_ORE to "Quặng Đồng Đá Bảng Sâu",
        Material.NETHER_GOLD_ORE to "Quặng Vàng Nether",
        Material.ANCIENT_DEBRIS to "Mảnh Vỡ Cổ Đại"
    )

    init {
        materialColors.forEach { (material, color) ->
            materialColorCache[material] = color
        }
        materialNames.forEach { (material, name) ->
            materialNameCache[material] = name
        }
    }

    fun getMaterialColor(material: Material): String {
        return materialColorCache.getOrPut(material) {
            "§f"
        }
    }

    fun getMaterialDisplayName(material: Material): String {
        return materialNameCache.getOrPut(material) {
            material.name
        }
    }

    fun clearCache() {
        materialColorCache.clear()
        materialNameCache.clear()
        materialColors.forEach { (material, color) ->
            materialColorCache[material] = color
        }
        materialNames.forEach { (material, name) ->
            materialNameCache[material] = name
        }
    }
}