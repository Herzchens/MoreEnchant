package com.herzchen.moreenchant.manager

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player

import java.util.*

class BossBarManager {
    private val bossBars = mutableMapOf<UUID, BossBar>()

    fun showBossBar(player: Player, message: String, color: BarColor) {
        val bossBar = bossBars.getOrPut(player.uniqueId) {
            Bukkit.createBossBar("", color, BarStyle.SOLID).apply {
                addPlayer(player)
            }
        }

        bossBar.setTitle(message)
        bossBar.color = color
        bossBar.isVisible = true
    }

    fun hideBossBar(player: Player) {
        bossBars[player.uniqueId]?.isVisible = false
    }

    fun removeAllBossBars() {
        bossBars.values.forEach { it.removeAll() }
        bossBars.clear()
    }
}