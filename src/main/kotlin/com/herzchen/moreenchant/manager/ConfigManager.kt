package com.herzchen.moreenchant.manager

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    data class ExplosionShape(
        val width: Int,
        val height: Int,
        val depth: Int,
        val chance: Double,
        val cooldown: Double,
    ) {
        val totalBlocks: Int
            get() = width * height * depth
    }

    data class DropGroup(
        val permission: String,
        val drops: Map<Material, Double>
    )

    private val _explosionShapes = mutableMapOf<String, ExplosionShape>()
    val explosionShapes: Map<String, ExplosionShape>
        get() = _explosionShapes.toMap()

    private val _dropGroups = mutableMapOf<String, DropGroup>()
    val dropGroups: Map<String, DropGroup>
        get() = _dropGroups.toMap()

    var maxBlocks = 1000
        private set
    var maxNearbyItems = 100
        private set
    var checkRadius = 10
        private set
    var virtualExplosionEnabled = false
        private set

    private val _blockWhitelist = mutableSetOf<Material>()
    val blockWhitelist: Set<Material>
        get() = _blockWhitelist.toSet()

    init {
        loadConfig()
    }

    fun loadConfig() {
        val configFile = File(plugin.dataFolder, "config.yml").apply {
            if (!exists()) plugin.saveResource("config.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(configFile)
        virtualExplosionEnabled = config.getBoolean("enchantments.virtualexplosion", false)

        if (virtualExplosionEnabled) {
            loadVirtualExplosionConfig()
        }
    }

    private fun loadVirtualExplosionConfig() {
        val veConfigFile = File(plugin.dataFolder, "enchantments/virtualexplosion.yml").apply {
            parentFile.mkdirs()
            if (!exists()) plugin.saveResource("enchantments/virtualexplosion.yml", false)
        }

        val veConfig = YamlConfiguration.loadConfiguration(veConfigFile)
        loadExplosionShapes(veConfig)
        loadDropGroups(veConfig)
        loadVESettings(veConfig)
        loadBlockWhitelist(veConfig)
    }

    private fun loadExplosionShapes(config: YamlConfiguration) {
        _explosionShapes.clear()
        val shapesSection = config.getConfigurationSection("explosion_shapes") ?: return

        for (key in shapesSection.getKeys(false)) {
            val width = shapesSection.getInt("$key.width", 3)
            val height = shapesSection.getInt("$key.height", 3)
            val depth = shapesSection.getInt("$key.depth", 3)
            val chance = shapesSection.getDouble("$key.chance", 100.0)
            val cooldown = shapesSection.getDouble("$key.cooldown", 0.0)
            _explosionShapes[key] = ExplosionShape(width, height, depth, chance, cooldown)
        }
    }

    private fun loadDropGroups(config: YamlConfiguration) {
        _dropGroups.clear()
        val dropsSection = config.getConfigurationSection("virtual_drops") ?: return

        for (groupKey in dropsSection.getKeys(false)) {
            val permission = dropsSection.getString("$groupKey.permission") ?: ""
            val dropsMap = mutableMapOf<Material, Double>()

            val dropsSubSection = dropsSection.getConfigurationSection("$groupKey.drops")
            dropsSubSection?.let { section ->
                for (materialKey in section.getKeys(false)) {
                    Material.matchMaterial(materialKey)?.let { material ->
                        val weight = if (section.isDouble(materialKey)) {
                            section.getDouble(materialKey)
                        } else {
                            section.getInt(materialKey).toDouble()
                        }
                        dropsMap[material] = weight
                    }
                }
            }

            _dropGroups[groupKey] = DropGroup(permission, dropsMap)
        }
    }

    private fun loadVESettings(config: YamlConfiguration) {
        maxBlocks = config.getInt("max_blocks", 1000)
        maxNearbyItems = config.getInt("anti_lag.max_nearby_items", 100)
        checkRadius = config.getInt("anti_lag.check_radius", 10)
    }

    private fun loadBlockWhitelist(config: YamlConfiguration) {
        _blockWhitelist.clear()
        val whitelist = config.getStringList("block_whitelist")

        if (whitelist.isEmpty()) {
            _blockWhitelist.add(Material.STONE)
            plugin.logger.warning("Block whitelist is empty, defaulting to STONE only")
        } else {
            whitelist.forEach { materialName ->
                Material.matchMaterial(materialName)?.let {
                    _blockWhitelist.add(it)
                } ?: run {
                    plugin.logger.warning("Invalid material name in block_whitelist: $materialName")
                }
            }
        }

        plugin.logger.info("Loaded ${_blockWhitelist.size} blocks to whitelist")
    }

    fun getExplosionShape(shapeKey: String) = _explosionShapes[shapeKey]

}