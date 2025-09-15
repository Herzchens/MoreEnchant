package com.herzchen.moreenchant

import com.herzchen.moreenchant.commands.MoeCommand
import com.herzchen.moreenchant.enchantments.Smelting
import com.herzchen.moreenchant.enchantments.VirtualExplosion
import com.herzchen.moreenchant.integration.ExtraStorageHook
import com.herzchen.moreenchant.listener.BlockBreakListener
import com.herzchen.moreenchant.listener.PlayerItemChangeListener
import com.herzchen.moreenchant.manager.*
import com.herzchen.moreenchant.utils.TabCompleter
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

class MoreEnchant : JavaPlugin() {
    lateinit var configManager: ConfigManager
    lateinit var enchantManager: EnchantManager
    lateinit var permissionManager: PermissionManager
    lateinit var extraStorageHook: ExtraStorageHook
    lateinit var bossBarManager: BossBarManager
    lateinit var performanceOptimizer: PerformanceOptimizer

    var virtualExplosion: VirtualExplosion? = null
    var smelting: Smelting? = null

    private fun showEnableArt() {
        this.server.consoleSender.sendMessage("")
        this.server.consoleSender.sendMessage("§c         /@@      /@@           /@@@@@@@@          ")
        this.server.consoleSender.sendMessage("§6        | @@@    /@@@          | @@_____/          ")
        this.server.consoleSender.sendMessage("§e        | @@@@  /@@@@  /@@@@@@ | @@       /@@@@@@@ ")
        this.server.consoleSender.sendMessage("§a        | @@ @@/@@ @@ /@@__  @@| @@@@@   | @@__  @@")
        this.server.consoleSender.sendMessage("§b        | @@  @@@| @@| @@  \\ @@| @@__/   | @@  \\ @@")
        this.server.consoleSender.sendMessage("§d        | @@\\  @ | @@| @@  | @@| @@      | @@  | @@")
        this.server.consoleSender.sendMessage("§5        | @@ \\/  | @@|  @@@@@@/| @@@@@@@@| @@  | @@")
        this.server.consoleSender.sendMessage("§9        |__/     |__/ \\______/ |________/|__/  |__/")
    }
    private fun showDisableArt() {
        this.server.consoleSender.sendMessage("")
        this.server.consoleSender.sendMessage("§c         /@@      /@@           /@@@@@@@@          ")
        this.server.consoleSender.sendMessage("§6        | @@@    /@@@          | @@_____/          ")
        this.server.consoleSender.sendMessage("§e        | @@@@  /@@@@  /@@@@@@ | @@       /@@@@@@@ ")
        this.server.consoleSender.sendMessage("§a        | @@ @@/@@ @@ /@@__  @@| @@@@@   | @@__  @@")
        this.server.consoleSender.sendMessage("§b        | @@  @@@| @@| @@  \\ @@| @@__/   | @@  \\ @@")
        this.server.consoleSender.sendMessage("§d        | @@\\  @ | @@| @@  | @@| @@      | @@  | @@")
        this.server.consoleSender.sendMessage("§5        | @@ \\/  | @@|  @@@@@@/| @@@@@@@@| @@  | @@")
        this.server.consoleSender.sendMessage("§9        |__/     |__/ \\______/ |________/|__/  |__/")
    }

    override fun onEnable() {
        configManager = ConfigManager(this)
        permissionManager = PermissionManager(this)
        enchantManager = EnchantManager()
        bossBarManager = BossBarManager()
        extraStorageHook = ExtraStorageHook(this)

        if (configManager.virtualExplosionEnabled) {
            virtualExplosion = VirtualExplosion(this)
            logger.info("VirtualExplosion đã được kích hoạt")
        }

        if (configManager.smeltingEnabled) {
            smelting = Smelting(this)
            logger.info("Smelting đã được kích hoạt")
        }

        performanceOptimizer = PerformanceOptimizer(this)

        server.pluginManager.registerEvents(PlayerItemChangeListener(this), this)
        server.pluginManager.registerEvents(BlockBreakListener(this), this)

        getCommand("moe")?.setExecutor(MoeCommand(this))
        getCommand("moe")?.tabCompleter = TabCompleter(this)

        showEnableArt()
        this.server.consoleSender.sendMessage("§a================================================================================")
        this.server.consoleSender.sendMessage("§e>> MoreEnchant v1.2.01 Đã bật!")
        this.server.consoleSender.sendMessage("§e>> Chạy trên phiên bản Minecraft ${server.version}")

        if (configManager.virtualExplosionEnabled) {
            this.server.consoleSender.sendMessage("§e>> VirtualExplosion: Đã kích hoạt")
        } else {
            this.server.consoleSender.sendMessage("§c>> VirtualExplosion: Đã tắt")
        }

        if (configManager.smeltingEnabled) {
            this.server.consoleSender.sendMessage("§e>> Smelting: Đã kích hoạt")
        } else {
            this.server.consoleSender.sendMessage("§c>> Smelting: Đã tắt")
        }

        server.scheduler.runTask(this, Runnable {
            for (player in server.onlinePlayers) {
                val item = player.inventory.itemInMainHand
                if (this@MoreEnchant.enchantManager.getEnchantShape(item) != null) {
                    val isPaused = virtualExplosion?.shouldPauseExplosion(player) ?: false
                    if (isPaused) {
                        bossBarManager.showBossBar(player,
                            "§eNổ ảo đã bị tạm dừng, vui lòng dọn dẹp item xung quanh bạn!",
                            org.bukkit.boss.BarColor.YELLOW)
                    } else {
                        bossBarManager.showBossBar(player,
                            "§aHiện đang kích hoạt Nổ Ảo",
                            org.bukkit.boss.BarColor.GREEN)
                    }
                }
            }
        })
    }

    override fun onDisable() {
        bossBarManager.removeAllBossBars()
        HandlerList.unregisterAll(this)
        showDisableArt()
        this.server.consoleSender.sendMessage("§c================================================================================")
        this.server.consoleSender.sendMessage("§c>> MoreEnchant Đã Tắt!")
        logger.info("MoreEnchant đã tắt!")
    }
}