package com.herzchen.moreenchant.utils

import com.herzchen.moreenchant.manager.ConfigManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class TabCompleter(private val configManager: ConfigManager) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (command.name.equals("moe", true)) {
            return when (args.size) {
                1 -> listOf("enchant", "disenchant", "reload", "help")
                    .filter { it.startsWith(args[0], true) }
                2 -> when (args[0].lowercase()) {
                    "enchant" -> configManager.explosionShapes.keys
                        .filter { it.startsWith(args[1], true) }
                        .toList()
                    else -> emptyList()
                }
                else -> emptyList()
            }
        }
        return emptyList()
    }
}