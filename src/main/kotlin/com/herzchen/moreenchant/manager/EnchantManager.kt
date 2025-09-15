package com.herzchen.moreenchant.manager

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.inventory.ItemStack

class EnchantManager : BaseEnchantmentManager() {
    private val vePrefixPlain = "Virtual Explosion "
    private val smeltingPrefixPlain = "Smelting "

    fun addEnchant(item: ItemStack, shapeKey: String): Boolean {
        return addLoreEnchant(item, vePrefixPlain, shapeKey, NamedTextColor.RED)
    }

    fun addSmeltingEnchant(item: ItemStack, level: String): Boolean {
        return addLoreEnchant(item, smeltingPrefixPlain, level, NamedTextColor.GOLD)
    }

    fun removeEnchant(item: ItemStack): Boolean {
        return removeLoreEnchant(item, vePrefixPlain)
    }

    fun removeSmeltingEnchant(item: ItemStack): Boolean {
        return removeLoreEnchant(item, smeltingPrefixPlain)
    }

    fun getEnchantShape(item: ItemStack): String? {
        return getLoreValue(item, vePrefixPlain)
    }

    fun getSmeltingLevel(item: ItemStack): String? {
        return getLoreValue(item, smeltingPrefixPlain)
    }
}