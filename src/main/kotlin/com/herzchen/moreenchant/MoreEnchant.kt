package com.herzchen.moreenchant

import com.herzchen.moreenchant.commands.MoeCommand
import com.herzchen.moreenchant.utils.TabCompleter
import com.herzchen.moreenchant.enchantments.VirtualExplosion
import com.herzchen.moreenchant.integration.ExtraStorageHook
import com.herzchen.moreenchant.listener.BlockBreakListener
import com.herzchen.moreenchant.manager.*
import com.herzchen.moreenchant.listener.PlayerItemChangeListener

import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

class MoreEnchant : JavaPlugin() {
    lateinit var configManager: ConfigManager
    lateinit var enchantManager: EnchantManager
    lateinit var permissionManager: PermissionManager
    lateinit var extraStorageHook: ExtraStorageHook
    lateinit var virtualExplosion: VirtualExplosion
    lateinit var bossBarManager: BossBarManager
    lateinit var performanceOptimizer: PerformanceOptimizer

    private val enableArt = """
        
         /@@      /@@           /@@@@@@@@          
        | @@@    /@@@          | @@_____/          
        | @@@@  /@@@@  /@@@@@@ | @@       /@@@@@@@ 
        | @@ @@/@@ @@ /@@__  @@| @@@@@   | @@__  @@
        | @@  @@@| @@| @@  \ @@| @@__/   | @@  \ @@
        | @@\  @ | @@| @@  | @@| @@      | @@  | @@
        | @@ \/  | @@|  @@@@@@/| @@@@@@@@| @@  | @@
        |__/     |__/ \______/ |________/|__/  |__/
    """.trimIndent()

    private val disableArt = """
        
         /@@      /@@           /@@@@@@@@          
        | @@@    /@@@          | @@_____/          
        | @@@@  /@@@@  /@@@@@@ | @@       /@@@@@@@ 
        | @@ @@/@@ @@ /@@__  @@| @@@@@   | @@__  @@
        | @@  @@@| @@| @@  \ @@| @@__/   | @@  \ @@
        | @@\  @ | @@| @@  | @@| @@      | @@  | @@
        | @@ \/  | @@|  @@@@@@/| @@@@@@@@| @@  | @@
        |__/     |__/ \______/ |________/|__/  |__/
    """.trimIndent()

    override fun onEnable() {
        configManager = ConfigManager(this)
        permissionManager = PermissionManager(this)
        enchantManager = EnchantManager()
        bossBarManager = BossBarManager()
        extraStorageHook = ExtraStorageHook(this)
        virtualExplosion = VirtualExplosion(this)
        performanceOptimizer = PerformanceOptimizer(this)
        server.pluginManager.registerEvents(PlayerItemChangeListener(this), this)


        server.pluginManager.registerEvents(BlockBreakListener(this), this)
        getCommand("moe")?.setExecutor(MoeCommand(this))
        getCommand("moe")?.tabCompleter = TabCompleter(configManager)

        this.server.consoleSender.sendMessage("§b$enableArt")
        this.server.consoleSender.sendMessage("§a================================================================================")
        this.server.consoleSender.sendMessage("§e>> MoreEnchant v1.1 Đã bật!")
        this.server.consoleSender.sendMessage("§e>> Chạy trên phiên bản Minecraft ${server.version}")

        checkWorldGuard()

        logger.info("MoreEnchant đã bật!")
        server.scheduler.runTask(this, Runnable {
            for (player in server.onlinePlayers) {
                val item = player.inventory.itemInMainHand
                if (this@MoreEnchant.enchantManager.getEnchantShape(item) != null) {
                    val isPaused = virtualExplosion.shouldPauseExplosion(player)
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
        this.server.consoleSender.sendMessage("§c$disableArt")
        this.server.consoleSender.sendMessage("§c================================================================================")
        this.server.consoleSender.sendMessage("§c>> MoreEnchant Đã Tắt!")
        logger.info("MoreEnchant đã tắt!")
    }

    private fun checkWorldGuard() {
        if (server.pluginManager.getPlugin("WorldGuard") == null) {
            logger.warning("================================================")
            logger.warning("WorldGuard không được tìm thấy!")
            logger.warning("Các khu vực được bảo vệ sẽ KHÔNG được áp dụng")
            logger.warning("cho hiệu ứng nổ ảo.")
            logger.warning("")
            logger.warning("Hãy cài đặt WorldGuard để sử dụng tính năng bảo vệ")
            logger.warning("================================================")
        } else {
            logger.info("Phát hiện WorldGuard, sẽ tôn trọng các vùng được bảo vệ.")
        }
    }
}