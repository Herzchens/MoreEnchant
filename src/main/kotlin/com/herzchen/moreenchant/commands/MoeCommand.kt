package com.herzchen.moreenchant.commands

import com.herzchen.moreenchant.MoreEnchant
import com.herzchen.moreenchant.utils.Helper

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class MoeCommand (plugin: MoreEnchant) : CommandExecutor {
    private val enchantManager = plugin.enchantManager
    private val config = plugin.configManager

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "enchant" -> handleEnchant(sender, args)
            "disenchant" -> handleDisenchant(sender)
            "reload" -> handleReload(sender)
            "help" -> {
                if (args.size > 1) {
                    Helper.showHelp(sender)
                } else {
                    Helper.showHelp(sender)
                }
            }
            else -> return false
        }
        return true
    }

    private fun handleEnchant(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("Chỉ người chơi có thể dùng lệnh này")
            return
        }

        if (!sender.hasPermission("moreenchant.enchant")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§eUsage: /moe enchant <shape>")
            sender.sendMessage("§6Available shapes: ${config.explosionShapes.keys.joinToString()}")
            return
        }

        val shapeKey = args[1]
        if (config.explosionShapes[shapeKey] == null) {
            sender.sendMessage("§cShape không tồn tại!")
            sender.sendMessage("§6Available shapes: ${config.explosionShapes.keys.joinToString()}")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (enchantManager.addEnchant(item, shapeKey)) {
            sender.sendMessage("§aĐã thêm phù phép Virtual Explosion ($shapeKey)!")
        } else {
            sender.sendMessage("§cKhông thể thêm phù phép vào vật phẩm này!")
        }
    }

    private fun handleDisenchant(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("Chỉ người chơi có thể dùng lệnh này")
            return
        }

        if (!sender.hasPermission("moreenchant.enchant")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (enchantManager.removeEnchant(item)) {
            sender.sendMessage("§aĐã gỡ phù phép Virtual Explosion!")
        } else {
            sender.sendMessage("§cVật phẩm không có phù phép này!")
        }
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("moreenchant.reload")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!")
            return
        }

        config.loadConfig()
        sender.sendMessage("§aCấu hình đã được nạp lại!")
    }

}