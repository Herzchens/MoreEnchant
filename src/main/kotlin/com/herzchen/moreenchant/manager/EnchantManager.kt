package com.herzchen.moreenchant.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class EnchantManager {
    private val vePrefixPlain = "Virtual Explosion "
    private val smeltingPrefixPlain = "Smelting "
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    fun addEnchant(item: ItemStack, shapeKey: String): Boolean {
        return addLoreEnchant(item, vePrefixPlain, shapeKey, NamedTextColor.RED)
    }

    fun addSmeltingEnchant(item: ItemStack, level: String): Boolean {
        return addLoreEnchant(item, smeltingPrefixPlain, level, NamedTextColor.GOLD)
    }

    private fun addLoreEnchant(item: ItemStack, prefix: String, value: String, color: NamedTextColor): Boolean {
        if (item.type == Material.AIR) return false

        val meta = item.itemMeta ?: return false
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        removeSpecificLore(lore, prefix)

        val newLine: Component = Component.text("$prefix$value", color)
            .decoration(TextDecoration.ITALIC, false)
        lore.add(newLine)

        meta.lore(lore)
        item.setItemMeta(meta)
        return true
    }

    fun removeEnchant(item: ItemStack): Boolean {
        return removeLoreEnchant(item, vePrefixPlain)
    }

    fun removeSmeltingEnchant(item: ItemStack): Boolean {
        return removeLoreEnchant(item, smeltingPrefixPlain)
    }

    private fun removeLoreEnchant(item: ItemStack, prefix: String): Boolean {
        val meta = item.itemMeta ?: return false
        val lore = meta.lore()?.toMutableList() ?: return false

        if (!removeSpecificLore(lore, prefix)) return false

        meta.lore(lore)
        item.setItemMeta(meta)
        return true
    }

    fun getEnchantShape(item: ItemStack): String? {
        return getLoreValue(item, vePrefixPlain)
    }

    fun getSmeltingLevel(item: ItemStack): String? {
        return getLoreValue(item, smeltingPrefixPlain)
    }

    private fun getLoreValue(item: ItemStack, prefix: String): String? {
        val lore = item.itemMeta?.lore() ?: return null

        val found = lore.firstOrNull { cmp ->
            plainSerializer.serialize(cmp).startsWith(prefix)
        } ?: return null

        val plain = plainSerializer.serialize(found)
        return plain.substring(prefix.length)
    }

    private fun removeSpecificLore(lore: MutableList<Component>, prefix: String): Boolean {
        return lore.removeAll { cmp ->
            plainSerializer.serialize(cmp).startsWith(prefix)
        }
    }
}