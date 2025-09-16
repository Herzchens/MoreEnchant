package com.herzchen.moreenchant.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

import org.bukkit.command.CommandSender

object Helper {
    fun showHelp(sender: CommandSender) {
        val title = Component.text("=== MoreEnchant Help ===")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD)

        val line1 = Component.text("/moe help ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Hiển thị trang này").color(NamedTextColor.GRAY))

        val line2 = Component.text("/moe enchant virtualexplosion <level> ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Thêm phù phép nổ ảo").color(NamedTextColor.GRAY))

        val line3 = Component.text("/moe enchant smelting <level> ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Thêm phù phép nung chảy").color(NamedTextColor.GRAY))

        val line4 = Component.text("/moe disenchant [enchantment] ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Gỡ phù phép").color(NamedTextColor.GRAY))

        val line5 = Component.text("/moe reload ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Tải lại cấu hình").color(NamedTextColor.GRAY))

        sender.sendMessage(title)
        sender.sendMessage(line1)
        sender.sendMessage(line2)
        sender.sendMessage(line3)
        sender.sendMessage(line4)
        sender.sendMessage(line5)
    }
}