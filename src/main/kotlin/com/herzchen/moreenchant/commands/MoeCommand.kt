package com.herzchen.moreenchant.commands

import com.herzchen.moreenchant.MoreEnchant
import com.herzchen.moreenchant.utils.Helper

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MoeCommand(private val plugin: MoreEnchant) : CommandExecutor {
    private val enchantManager = plugin.enchantManager

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "enchant" -> handleEnchant(sender, args)
            "disenchant" -> handleDisenchant(sender, args)
            "reload" -> handleReload(sender)
            "help" -> Helper.showHelp(sender)
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
            sender.sendMessage("§eUsage: /moe enchant <enchantment> [level]")
            sender.sendMessage("§6Available enchantments: virtualexplosion, smelting")
            return
        }

        val enchantmentName = args[1].lowercase()

        when (enchantmentName) {
            "virtualexplosion" -> handleVirtualExplosionEnchant(sender, args)
            "smelting" -> handleSmeltingEnchant(sender, args)
            else -> {
                sender.sendMessage("§cEnchantment không tồn tại!")
                sender.sendMessage("§6Available enchantments: virtualexplosion, smelting")
            }
        }
    }

    private fun handleVirtualExplosionEnchant(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage("§eUsage: /moe enchant virtualexplosion <level>")
            player.sendMessage("§6Available levels: ${plugin.configManager.explosionShapes.keys.joinToString()}")
            return
        }

        val level = args[2]
        if (plugin.configManager.explosionShapes[level] == null) {
            player.sendMessage("§cLevel không tồn tại!")
            player.sendMessage("§6Available levels: ${plugin.configManager.explosionShapes.keys.joinToString()}")
            return
        }

        val item = player.inventory.itemInMainHand
        if (enchantManager.addEnchant(item, level)) {
            player.sendMessage("§aĐã thêm phù phép Virtual Explosion ($level)!")
        } else {
            player.sendMessage("§cKhông thể thêm phù phép vào vật phẩm này!")
        }
    }

    private fun handleSmeltingEnchant(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage("§eUsage: /moe enchant smelting <level>")
            player.sendMessage("§6Available levels: ${plugin.smelting!!.getAllLevels().joinToString()}")
            return
        }

        val level = args[2]
        if (plugin.smelting!!.getLevelConfig(level) == null) {
            player.sendMessage("§cLevel không tồn tại!")
            player.sendMessage("§6Available levels: ${plugin.smelting!!.getAllLevels().joinToString()}")
            return
        }

        val item = player.inventory.itemInMainHand
        if (enchantManager.addSmeltingEnchant(item, level)) {
            player.sendMessage("§aĐã thêm phù phép Smelting ($level)!")
        } else {
            player.sendMessage("§cKhông thể thêm phù phép vào vật phẩm này!")
        }
    }

    private fun handleDisenchant(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("Chỉ người chơi có thể dùng lệnh này")
            return
        }

        if (!sender.hasPermission("moreenchant.enchant")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!")
            return
        }

        if (args.size > 1) {
            val enchantmentName = args[1].lowercase()
            val item = sender.inventory.itemInMainHand

            when (enchantmentName) {
                "virtualexplosion" -> {
                    if (enchantManager.removeEnchant(item)) {
                        sender.sendMessage("§aĐã gỡ phù phép Virtual Explosion!")
                    } else {
                        sender.sendMessage("§cVật phẩm không có phù phép này!")
                    }
                    return
                }
                "smelting" -> {
                    if (enchantManager.removeSmeltingEnchant(item)) {
                        sender.sendMessage("§aĐã gỡ phù phép Smelting!")
                    } else {
                        sender.sendMessage("§cVật phẩm không có phù phép này!")
                    }
                    return
                }
                else -> {
                    sender.sendMessage("§cEnchantment không tồn tại!")
                    return
                }
            }
        }

        val item = sender.inventory.itemInMainHand
        var removed = false

        if (enchantManager.removeEnchant(item)) {
            sender.sendMessage("§aĐã gỡ phù phép Virtual Explosion!")
            removed = true
        }

        if (enchantManager.removeSmeltingEnchant(item)) {
            sender.sendMessage("§aĐã gỡ phù phép Smelting!")
            removed = true
        }

        if (!removed) {
            sender.sendMessage("§cVật phẩm không có phù phép nào!")
        }
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("moreenchant.reload")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!")
            return
        }

        plugin.configManager.loadConfig()
        plugin.smelting!!.loadConfig()
        sender.sendMessage("§aCấu hình đã được nạp lại!")
    }
}