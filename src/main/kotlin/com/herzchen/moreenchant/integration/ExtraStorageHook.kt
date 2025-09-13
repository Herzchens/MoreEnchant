package com.herzchen.moreenchant.integration

import com.herzchen.moreenchant.MoreEnchant

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import java.lang.reflect.Method
import java.util.*

import kotlin.math.min

class ExtraStorageHook(private val plugin: MoreEnchant) {
    private var storageAPI: Any? = null
    private var getUserMethod: Method? = null
    private var getStorageMethod: Method? = null
    private var addItemMethod: Method? = null
    private var isEnabled = false

    init {
        setupExtraStorageHook()
    }

    @Synchronized
    fun getStorageInfo(player: Player): Pair<Long, Long>? {
        if (!isEnabled) return null

        return try {
            val user = getUserMethod!!.invoke(storageAPI, player.uniqueId) ?: return null
            val storage = getStorageMethod!!.invoke(user) ?: return null

            val getUsedSpaceMethod = storage.javaClass.getMethod("getUsedSpace")
            val getSpaceMethod = storage.javaClass.getMethod("getSpace")
            val isMaxSpaceMethod = storage.javaClass.getMethod("isMaxSpace")

            val used = getUsedSpaceMethod.invoke(storage) as Long
            val capacity = getSpaceMethod.invoke(storage) as Long
            val isMaxSpace = isMaxSpaceMethod.invoke(storage) as Boolean

            if (isMaxSpace && used >= capacity) {
                return Pair(used, Long.MAX_VALUE)
            }

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

            getUserMethod = apiClass.getMethod("getUser", UUID::class.java)

            val userClass = Class.forName("me.hsgamer.extrastorage.api.user.User")
            getStorageMethod = userClass.getMethod("getStorage")

            val storageClass = Class.forName("me.hsgamer.extrastorage.api.storage.Storage")
            addItemMethod = storageClass.getMethod("add", Any::class.java, Long::class.java)

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
            val user = getUserMethod!!.invoke(storageAPI, player.uniqueId) ?: return Pair(emptyList(), items)
            val storage = getStorageMethod!!.invoke(user) ?: return Pair(emptyList(), items)

            val storageInfo = getStorageInfo(player)
            var remainingSpace = Long.MAX_VALUE

            if (storageInfo != null) {
                val (used, capacity) = storageInfo
                remainingSpace = if (capacity == Long.MAX_VALUE) Long.MAX_VALUE else capacity - used

                if (remainingSpace <= 0) {
                    return Pair(emptyList(), items)
                }
            }

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
                    addItemMethod!!.invoke(storage, item.type, amountToAdd)
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
            val user = getUserMethod!!.invoke(storageAPI, player.uniqueId)
            if (user == null) {
                plugin.logger.warning("Lỗi lấy tên người chơi ${player.name}")
                return false
            }

            val storage = getStorageMethod!!.invoke(user)
            if (storage == null) {
                plugin.logger.warning("Lỗi lấy kho cho người chơi ${player.name}")
                return false
            }

            val getStatusMethod = storage.javaClass.getMethod("getStatus")
            getStatusMethod.invoke(storage) as Boolean
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

}