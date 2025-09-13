package com.herzchen.moreenchant.manager

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class EnchantManager {
    private val vePrefixPlain = "Virtual Explosion "
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    fun addEnchant(item: ItemStack, shapeKey: String): Boolean {
        if (item.type == Material.AIR) return false

        val meta = item.itemMeta ?: return false
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        removeEnchantLore(lore)

        val newLine: Component = Component.text("$vePrefixPlain$shapeKey", NamedTextColor.BLUE)
        lore.add(newLine)

        meta.lore(lore)
        item.setItemMeta(meta)
        return true
    }

    fun removeEnchant(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val lore = meta.lore()?.toMutableList() ?: return false

        if (!removeEnchantLore(lore)) return false

        meta.lore(lore)
        item.setItemMeta(meta)
        return true
    }

    fun getEnchantShape(item: ItemStack): String? {
        val lore = item.itemMeta?.lore() ?: return null

        val found = lore.firstOrNull { cmp ->
            plainSerializer.serialize(cmp).startsWith(vePrefixPlain)
        } ?: return null

        val plain = plainSerializer.serialize(found)
        return plain.substring(vePrefixPlain.length)
    }

    private fun removeEnchantLore(lore: MutableList<Component>): Boolean {
        return lore.removeAll { cmp ->
            plainSerializer.serialize(cmp).startsWith(vePrefixPlain)
        }
    }
}
