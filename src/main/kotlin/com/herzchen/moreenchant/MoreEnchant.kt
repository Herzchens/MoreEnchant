package com.herzchen.moreenchant

import com.herzchen.moreenchant.commands.MoeCommand
import com.herzchen.moreenchant.utils.TabCompleter
import com.herzchen.moreenchant.enchantments.virtualexplosion.manager.VirtualExplosionManager
import com.herzchen.moreenchant.integration.ExtraStorageHook
import com.herzchen.moreenchant.listener.BlockBreakListener
import com.herzchen.moreenchant.manager.*
import com.herzchen.moreenchant.listener.PlayerItemChangeListener
import org.bukkit.plugin.java.JavaPlugin

class MoreEnchant : JavaPlugin() {
    lateinit var configManager: ConfigManager
    lateinit var enchantManager: EnchantManager
    lateinit var permissionManager: PermissionManager
    lateinit var extraStorageHook: ExtraStorageHook
    lateinit var virtualExplosionManager: VirtualExplosionManager
    lateinit var bossBarManager: BossBarManager

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
        virtualExplosionManager = VirtualExplosionManager(this)
        server.pluginManager.registerEvents(PlayerItemChangeListener(this), this)


        server.pluginManager.registerEvents(BlockBreakListener(this), this)
        getCommand("moe")?.setExecutor(MoeCommand(this))
        getCommand("moe")?.tabCompleter = TabCompleter(configManager)

        this.server.consoleSender.sendMessage("§b$enableArt")
        this.server.consoleSender.sendMessage("§a================================================================================")
        this.server.consoleSender.sendMessage("§e>> MoreEnchant v1.0 Enabled!")
        this.server.consoleSender.sendMessage("§e>> Running on Minecraft ${server.version}")

        checkWorldGuard()

        logger.info("MoreEnchant enabled!")
        server.scheduler.runTask(this, Runnable {
            for (player in server.onlinePlayers) {
                val item = player.inventory.itemInMainHand
                if (this@MoreEnchant.enchantManager.getEnchantShape(item) != null) {
                    val isPaused = virtualExplosionManager.shouldPauseExplosion(player.location)
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
        this.server.consoleSender.sendMessage("§c$disableArt")
        this.server.consoleSender.sendMessage("§c================================================================================")
        this.server.consoleSender.sendMessage("§c>> MoreEnchant Disabled!")
        logger.info("MoreEnchant disabled!")
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
            logger.info("WorldGuard detected! Protected regions will be respected.")
        }
    }
}