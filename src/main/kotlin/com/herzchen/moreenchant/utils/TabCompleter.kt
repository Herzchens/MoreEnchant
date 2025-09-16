package com.herzchen.moreenchant.utils

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class TabCompleter(private val plugin: MoreEnchant) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (command.name.equals("moe", true)) {
            val hasEnchantPermission = sender.hasPermission("moreenchant.enchant")
            val hasReloadPermission = sender.hasPermission("moreenchant.reload")

            return when (args.size) {
                1 -> {
                    val suggestions = mutableListOf<String>()
                    if (hasEnchantPermission) {
                        suggestions.add("enchant")
                        suggestions.add("disenchant")
                    }
                    if (hasReloadPermission) {
                        suggestions.add("reload")
                    }
                    suggestions.add("help")

                    suggestions.filter { it.startsWith(args[0], true) }
                }
                2 -> when (args[0].lowercase()) {
                    "enchant" -> {
                        if (!hasEnchantPermission) return emptyList()
                        listOf("virtualexplosion", "smelting")
                            .filter { it.startsWith(args[1], true) }
                    }
                    "disenchant" -> {
                        if (!hasEnchantPermission) return emptyList()
                        listOf("virtualexplosion", "smelting")
                            .filter { it.startsWith(args[1], true) }
                    }
                    else -> emptyList()
                }
                3 -> when (args[0].lowercase()) {
                    "enchant" -> {
                        if (!hasEnchantPermission) return emptyList()
                        when {
                            args[1].equals("virtualexplosion", ignoreCase = true) -> {
                                plugin.configManager.explosionShapes.keys
                                    .filter { it.startsWith(args[2], true) }
                                    .toList()
                            }
                            args[1].equals("smelting", ignoreCase = true) -> {
                                plugin.smelting!!.getAllLevels()
                                    .filter { it.startsWith(args[2], true) }
                                    .toList()
                            }
                            else -> emptyList()
                        }
                    }
                    else -> emptyList()
                }
                else -> emptyList()
            }
        }
        return emptyList()
    }
}