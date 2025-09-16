package com.herzchen.moreenchant.manager

import com.herzchen.moreenchant.MoreEnchant
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

import java.io.File
import java.nio.file.*
import kotlin.random.Random

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
        val drops: Map<Material, Double>,
        val cumulativeWeights: List<Pair<Material, Double>>,
        val aliasTable: AliasTable? = null
    )

    data class AliasTable(
        val items: List<Material>,
        val alias: IntArray,
        val prob: DoubleArray
    ) {
        fun sample(): Material {
            val index = Random.nextInt(items.size)
            return if (Random.nextDouble() < prob[index]) items[index] else items[alias[index]]
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AliasTable

            if (items != other.items) return false
            if (!alias.contentEquals(other.alias)) return false
            if (!prob.contentEquals(other.prob)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = items.hashCode()
            result = 31 * result + alias.contentHashCode()
            result = 31 * result + prob.contentHashCode()
            return result
        }
    }

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
    var virtualExplosionEnabled = true
        private set
    var smeltingEnabled = true
        private set

    private val _blockWhitelist = mutableSetOf<Material>()
    val blockWhitelist: Set<Material>
        get() = _blockWhitelist.toSet()

    private val lastModifiedMap = mutableMapOf<String, Long>()

    private val watcher = FileSystems.getDefault().newWatchService()

    init {
        loadConfig()
        val path = plugin.dataFolder.toPath()
        path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)
        (plugin as MoreEnchant).asyncExecutor.submit { watchForChanges() }
    }

    private fun watchForChanges() {
        while (true) {
            val key = try { watcher.take() } catch (_: InterruptedException) { return }
            key?.pollEvents()?.forEach { event ->
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    loadConfig()
                }
            }
            key?.reset()
        }
    }

    fun loadConfig(onlySections: List<String> = emptyList()) {
        val configFile = File(plugin.dataFolder, "config.yml").apply {
            if (!exists()) plugin.saveResource("config.yml", false)
        }

        if (onlySections.isEmpty() || onlySections.contains("main")) {
            if (shouldReload("main", configFile)) {
                loadMainConfig(configFile)
                lastModifiedMap["main"] = configFile.lastModified()
            }
        }

        if (virtualExplosionEnabled && (onlySections.isEmpty() || onlySections.contains("virtualexplosion"))) {
            val veConfigFile = File(plugin.dataFolder, "enchantments/virtualexplosion.yml")
            if (shouldReload("virtualexplosion", veConfigFile)) {
                loadVirtualExplosionConfig()
                if (veConfigFile.exists()) {
                    lastModifiedMap["virtualexplosion"] = veConfigFile.lastModified()
                }
            }
        }

        if (smeltingEnabled && (onlySections.isEmpty() || onlySections.contains("smelting"))) {
            val smeltingConfigFile = File(plugin.dataFolder, "enchantments/smelting.yml")
            if (shouldReload("smelting", smeltingConfigFile)) {
                loadSmeltingConfig()
                if (smeltingConfigFile.exists()) {
                    lastModifiedMap["smelting"] = smeltingConfigFile.lastModified()
                }
            }
        }
    }

    fun forceReload(onlySections: List<String> = emptyList()) {
        if (onlySections.isEmpty()) {
            lastModifiedMap.clear()
        } else {
            onlySections.forEach { section ->
                lastModifiedMap.remove(section)
            }
        }
        loadConfig(onlySections)
    }

    private fun shouldReload(section: String, configFile: File): Boolean {
        if (!configFile.exists()) return false

        val currentModified = configFile.lastModified()
        val lastModified = lastModifiedMap[section] ?: 0L

        return currentModified != lastModified
    }

    private fun loadMainConfig(configFile: File) {
        val config = YamlConfiguration.loadConfiguration(configFile)
        virtualExplosionEnabled = config.getBoolean("enchantments.virtualexplosion", true)
        smeltingEnabled = config.getBoolean("enchantments.smelting", true)

        plugin.logger.info("Main config loaded successfully")
    }

    private fun loadVirtualExplosionConfig() {
        File(plugin.dataFolder, "enchantments").mkdirs()

        val veConfigFile = File(plugin.dataFolder, "enchantments/virtualexplosion.yml").apply {
            if (!exists()) plugin.saveResource("enchantments/virtualexplosion.yml", false)
        }

        val veConfig = YamlConfiguration.loadConfiguration(veConfigFile)
        loadExplosionShapes(veConfig)
        loadDropGroups(veConfig)
        loadVESettings(veConfig)
        loadBlockWhitelist(veConfig)

        plugin.logger.info("VirtualExplosion config loaded successfully")
    }

    private fun loadSmeltingConfig() {
        File(plugin.dataFolder, "enchantments").mkdirs()

        File(plugin.dataFolder, "enchantments/smelting.yml").apply {
            if (!exists()) plugin.saveResource("enchantments/smelting.yml", false)
        }

        plugin.logger.info("Smelting config loaded successfully")
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

        plugin.logger.info("Loaded ${_explosionShapes.size} explosion shapes")
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

            var cumulative = 0.0
            val cumulativeWeights = dropsMap.map { (material, weight) ->
                cumulative += weight
                material to cumulative
            }

            val aliasTable = createAliasTable(dropsMap)
            _dropGroups[groupKey] = DropGroup(permission, dropsMap, cumulativeWeights, aliasTable)
        }

        plugin.logger.info("Loaded ${_dropGroups.size} drop groups")
    }

    private fun createAliasTable(drops: Map<Material, Double>): AliasTable? {
        val n = drops.size
        if (n == 0) return null

        val items = drops.keys.toList()
        val probabilities = drops.values.toDoubleArray()
        val alias = IntArray(n)
        val prob = DoubleArray(n)

        val sum = probabilities.sum()
        probabilities.forEachIndexed { i, v -> probabilities[i] = v * n / sum }

        val small = mutableListOf<Int>()
        val large = mutableListOf<Int>()

        probabilities.forEachIndexed { i, p ->
            if (p < 1.0) small.add(i) else large.add(i)
        }

        while (small.isNotEmpty() && large.isNotEmpty()) {
            val l = small.removeAt(0)
            val g = large.removeAt(0)
            prob[l] = probabilities[l]
            alias[l] = g
            probabilities[g] = probabilities[g] + probabilities[l] - 1.0
            if (probabilities[g] < 1.0) small.add(g) else large.add(g)
        }

        while (small.isNotEmpty()) {
            val l = small.removeAt(0)
            prob[l] = 1.0
        }

        while (large.isNotEmpty()) {
            val g = large.removeAt(0)
            prob[g] = 1.0
        }

        return AliasTable(items, alias, prob)
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

    fun hasConfigChanged(): Boolean {
        val configFile = File(plugin.dataFolder, "config.yml")
        val veConfigFile = File(plugin.dataFolder, "enchantments/virtualexplosion.yml")
        val smeltingConfigFile = File(plugin.dataFolder, "enchantments/smelting.yml")

        return shouldReload("main", configFile) ||
                (virtualExplosionEnabled && shouldReload("virtualexplosion", veConfigFile)) ||
                (smeltingEnabled && shouldReload("smelting", smeltingConfigFile))
    }

    fun getConfigStatus(): Map<String, String> {
        val status = mutableMapOf<String, String>()

        val configFile = File(plugin.dataFolder, "config.yml")
        if (configFile.exists()) {
            val lastLoaded = lastModifiedMap["main"] ?: 0L
            val current = configFile.lastModified()
            status["main"] = if (current == lastLoaded) "Up to date" else "Modified"
        }

        val veConfigFile = File(plugin.dataFolder, "enchantments/virtualexplosion.yml")
        if (virtualExplosionEnabled && veConfigFile.exists()) {
            val lastLoaded = lastModifiedMap["virtualexplosion"] ?: 0L
            val current = veConfigFile.lastModified()
            status["virtualexplosion"] = if (current == lastLoaded) "Up to date" else "Modified"
        }

        val smeltingConfigFile = File(plugin.dataFolder, "enchantments/smelting.yml")
        if (smeltingEnabled && smeltingConfigFile.exists()) {
            val lastLoaded = lastModifiedMap["smelting"] ?: 0L
            val current = smeltingConfigFile.lastModified()
            status["smelting"] = if (current == lastLoaded) "Up to date" else "Modified"
        }

        return status
    }
}