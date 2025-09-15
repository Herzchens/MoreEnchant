package com.herzchen.moreenchant.manager

import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material

open class BaseEnchantmentManager {
    protected val plainSerializer = PlainTextComponentSerializer.plainText()

    protected fun addLoreEnchant(
        item: ItemStack,
        prefix: String,
        value: String,
        color: NamedTextColor
    ): Boolean {
        if (!isValidItem(item)) return false

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

    protected fun removeLoreEnchant(item: ItemStack, prefix: String): Boolean {
        val meta = item.itemMeta ?: return false
        val lore = meta.lore()?.toMutableList() ?: return false

        if (!removeSpecificLore(lore, prefix)) return false

        meta.lore(lore)
        item.setItemMeta(meta)
        return true
    }

    protected fun getLoreValue(item: ItemStack, prefix: String): String? {
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

    private fun isValidItem(item: ItemStack): Boolean {
        return item.type != Material.AIR
    }
}