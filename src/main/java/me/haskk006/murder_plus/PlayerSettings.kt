package me.haskk006.murder_plus

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.inventory.*
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta

class PlayerSettings(private val plugin: JavaPlugin) : Listener {
    var invincible = true

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        // ブロック設置をキャンセル
        event.isCancelled = true
    }
    // ブロック破壊を無効
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        // クラフトイベントをキャンセル
        event.isCancelled = true
    }
    // プレイヤーを無敵状態にする
    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (invincible && event.entity is Player) {
            event.isCancelled = true
        }
        // エンティティを破壊不可能にする
        if (event.entity !is Player) {
            event.isCancelled = true
        }
    }
    // ガイドブックへのクリックを禁止する
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem
        if (clickedItem != null) {
            if (event.currentItem?.type == Material.WRITTEN_BOOK || event.currentItem?.type == Material.FILLED_MAP || event.currentItem?.type == Material.TRIDENT || event.currentItem?.type == Material.TNT || event.currentItem?.type == Material.TRIPWIRE_HOOK || event.currentItem?.type == Material.BARRIER || event.currentItem?.type == Material.INK_SAC || event.currentItem?.type == Material.TOTEM_OF_UNDYING) {
                event.isCancelled = true
            }
        }
    }
    // 特定アイテム（ガイドブック、マップ、弓、剣）を破棄不可にする
    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        val meta = item.itemMeta
        if (meta != null) {
            if (item.type == Material.WRITTEN_BOOK) { // bug
                event.isCancelled = true
            } else if (item.type == Material.BOW || item.type == Material.TRIDENT || item.type == Material.TNT || item.type == Material.INK_SAC || item.type == Material.TOTEM_OF_UNDYING) {
                event.isCancelled = true
            } else if (item.type == Material.TRIPWIRE_HOOK) {
                event.isCancelled = true
            } else if (item.type == Material.FILLED_MAP) {
                event.isCancelled = true
            } else if (item.type == Material.BARRIER) {
                event.isCancelled = true
            }
        }
    }
    // 奈落落下対策
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val world = player.world
        if (player.location.y <= -60) {
            player.teleport(Location(world, 0.0, 1.0, 0.0))
        }
    }
    // アーマースタンドからアイテムを取得できないようにする
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked.type == org.bukkit.entity.EntityType.ARMOR_STAND || event.rightClicked.type == org.bukkit.entity.EntityType.PAINTING) {
            event.isCancelled = true
        }
    }
    // 特殊アイテムをオフハンドに持てないようにする
    @EventHandler
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val offHandItem = event.offHandItem?.type
        if (offHandItem == Material.WRITTEN_BOOK || offHandItem == Material.FILLED_MAP || offHandItem == Material.BOW || offHandItem == Material.TRIDENT || offHandItem == Material.TNT || offHandItem == Material.TRIPWIRE_HOOK || offHandItem == Material.BARRIER || offHandItem == Material.INK_SAC || offHandItem == Material.TOTEM_OF_UNDYING) {
            event.isCancelled = true
        }
    }
    // ベッドへのインタラクトを無効
    @EventHandler
    fun onBedEnter(event: PlayerBedEnterEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val consumedItem = event.item
        if (consumedItem.type == Material.POTION) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.inventory.removeItem(ItemStack(Material.GLASS_BOTTLE))
            }, 2L)
        }
    }
}

