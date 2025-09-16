package com.herzchen.moreenchant.integration

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap

import kotlin.math.min

class ExtraStorageHook(private val plugin: MoreEnchant) {
    private var storageAPI: Any? = null
    private var isEnabled = false

    private val methodCache = ConcurrentHashMap<String, Method>()

    init {
        setupExtraStorageHook()
    }

    private fun getCachedMethod(className: String, methodName: String, vararg parameterTypes: Class<*>): Method? {
        val cacheKey = "$className.$methodName.${parameterTypes.joinToString { it.simpleName }}"
        return methodCache.getOrPut(cacheKey) {
            try {
                Class.forName(className).getMethod(methodName, *parameterTypes)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to cache method $cacheKey: ${e.message}")
                throw e
            }
        }
    }

    @Synchronized
    fun getStorageInfo(player: Player): Pair<Long, Long>? {
        if (!isEnabled) return null

        return try {
            val getUser = getCachedMethod("me.hsgamer.extrastorage.api.StorageAPI", "getUser", UUID::class.java)
            val user = getUser?.invoke(storageAPI, player.uniqueId) ?: return null

            val getStorage = getCachedMethod("me.hsgamer.extrastorage.api.user.User", "getStorage")
            val storage = getStorage?.invoke(user) ?: return null

            val getUsedSpace = getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "getUsedSpace")
            val getSpace = getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "getSpace")

            val used = getUsedSpace?.invoke(storage) as Long
            val capacity = getSpace?.invoke(storage) as Long

            Pair(used, capacity)
        } catch (e: Exception) {
            plugin.logger.warning("Lỗi khi lấy thông tin của kho: ${e.message}")
            null
        }
    }

    private fun setupExtraStorageHook() {
        try {
            val extraStoragePlugin = Bukkit.getPluginManager().getPlugin("ExtraStorage")
            if (extraStoragePlugin == null) {
                plugin.logger.info("ExtraStorage không được phát hiện, sử dụng drop vanilla")
                return
            }

            val apiClass = Class.forName("me.hsgamer.extrastorage.api.StorageAPI")
            val getInstanceMethod = apiClass.getMethod("getInstance")
            storageAPI = getInstanceMethod.invoke(null)

            getCachedMethod("me.hsgamer.extrastorage.api.StorageAPI", "getUser", UUID::class.java)
            getCachedMethod("me.hsgamer.extrastorage.api.user.User", "getStorage")
            getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "add", Any::class.java, Long::class.java)
            getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "getUsedSpace")
            getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "getSpace")
            getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "getStatus")

            isEnabled = true
            plugin.logger.info("Đã liên kết thành công với ExtraStorage API")
        } catch (e: Exception) {
            plugin.logger.warning("Liên kết thất bại với ExtraStorage API do: ${e.message}")
            e.printStackTrace()
        }
    }

    @Synchronized
    fun addToStorage(player: Player, items: List<ItemStack>): Pair<List<ItemStack>, List<ItemStack>> {
        if (!isEnabled) return Pair(emptyList(), items)

        val successful = mutableListOf<ItemStack>()
        val failed = mutableListOf<ItemStack>()

        try {
            val getUser = getCachedMethod("me.hsgamer.extrastorage.api.StorageAPI", "getUser", UUID::class.java)
            val user = getUser?.invoke(storageAPI, player.uniqueId) ?: return Pair(emptyList(), items)

            val getStorage = getCachedMethod("me.hsgamer.extrastorage.api.user.User", "getStorage")
            val storage = getStorage?.invoke(user) ?: return Pair(emptyList(), items)

            val storageInfo = getStorageInfo(player)
            var remainingSpace = Long.MAX_VALUE

            if (storageInfo != null) {
                val (used, capacity) = storageInfo
                remainingSpace = if (capacity == Long.MAX_VALUE) Long.MAX_VALUE else capacity - used

                if (remainingSpace <= 0) {
                    return Pair(emptyList(), items)
                }
            }

            val addItem = getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "add", Any::class.java, Long::class.java)

            for (item in items) {
                if (remainingSpace <= 0) {
                    failed.add(item)
                    continue
                }

                val amountToAdd = if (remainingSpace == Long.MAX_VALUE) {
                    item.amount.toLong()
                } else {
                    min(item.amount.toLong(), remainingSpace)
                }

                if (amountToAdd > 0) {
                    addItem?.invoke(storage, item.type, amountToAdd)
                    successful.add(ItemStack(item.type, amountToAdd.toInt()))

                    if (remainingSpace != Long.MAX_VALUE) {
                        remainingSpace -= amountToAdd
                    }
                } else {
                    failed.add(item)
                }
            }

            return Pair(successful, failed)
        } catch (e: Exception) {
            plugin.logger.warning("Thêm vật phẩm thất bại: ${e.message}")
            return Pair(emptyList(), items)
        }
    }

    fun isAvailable(): Boolean {
        return isEnabled
    }

    @Synchronized
    fun hasAutoPickup(player: Player): Boolean {
        if (!isEnabled) return false

        return try {
            val getUser = getCachedMethod("me.hsgamer.extrastorage.api.StorageAPI", "getUser", UUID::class.java)
            val user = getUser?.invoke(storageAPI, player.uniqueId)
            if (user == null) {
                plugin.logger.warning("Lỗi lấy tên người chơi ${player.name}")
                return false
            }

            val getStorage = getCachedMethod("me.hsgamer.extrastorage.api.user.User", "getStorage")
            val storage = getStorage?.invoke(user)
            if (storage == null) {
                plugin.logger.warning("Lỗi lấy kho cho người chơi ${player.name}")
                return false
            }

            val getStatus = getCachedMethod("me.hsgamer.extrastorage.api.storage.Storage", "getStatus")
            getStatus?.invoke(storage) as Boolean
        } catch (e: Exception) {
            plugin.logger.warning("Có lỗi xảy ra khi kiểm tra trạng thái bật/tắt: ${e.message}")
            false
        }
    }

    @Synchronized
    fun isStorageFull(player: Player): Boolean {
        if (!isEnabled) return false

        val storageInfo = getStorageInfo(player)
        return storageInfo?.let { (used, capacity) ->
            if (capacity == Long.MAX_VALUE) false
            else used >= capacity
        } ?: false
    }

    fun clearMethodCache() {
        methodCache.clear()
        plugin.logger.info("Method cache đã được xóa")
    }
}