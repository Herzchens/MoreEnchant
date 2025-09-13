package com.herzchen.moreenchant.listener

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerItemChangeListener(private val plugin: MoreEnchant) : Listener {

    @EventHandler
    fun onItemHeldChange(event: PlayerItemHeldEvent) {
        updateBossBarForPlayer(event.player)
    }

    @EventHandler
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        updateBossBarForPlayer(event.player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        updateBossBarForPlayer(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.bossBarManager.hideBossBar(event.player)
    }

    private fun isHoldingEnchantedTool(player: org.bukkit.entity.Player): Boolean {
        val item = player.inventory.itemInMainHand
        return plugin.enchantManager.getEnchantShape(item) != null
    }

    private fun updateBossBarForPlayer(player: org.bukkit.entity.Player) {
        if (isHoldingEnchantedTool(player)) {
            val shouldPauseDueToStorage = plugin.extraStorageHook.isAvailable() &&
                    plugin.extraStorageHook.hasAutoPickup(player) &&
                    plugin.extraStorageHook.isStorageFull(player)

            if (shouldPauseDueToStorage) {
                plugin.bossBarManager.showBossBar(player,
                    "§cKho đã đầy! Không thể đập thêm khối.",
                    org.bukkit.boss.BarColor.RED)
                return
            }

            val isPaused = plugin.virtualExplosionManager.shouldPauseExplosion(player.location)
            if (isPaused) {
                plugin.bossBarManager.showBossBar(player,
                    "§cNổ ảo đã bị tạm dừng, vui lòng dọn dẹp vật phẩm xung quanh bạn!",
                    org.bukkit.boss.BarColor.RED)
            } else {
                plugin.bossBarManager.showBossBar(player,
                    "§aHiện đang kích hoạt Nổ Ảo",
                    org.bukkit.boss.BarColor.GREEN)
            }
        } else {
            plugin.bossBarManager.hideBossBar(player)
        }
    }
}