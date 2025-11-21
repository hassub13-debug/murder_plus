package me.haskk006.murder_plus

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import org.bukkit.block.Chest
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class LootChest(private val plugin: JavaPlugin, private val gameDirection: GameDirection) : Listener {
    lateinit var openedChests:  MutableMap<String, BukkitRunnable>

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        openedChests = gameDirection.openedChests
        val holder = event.inventory.holder
        if (holder is Chest) {
            val chest = event.inventory.holder as Chest
            val chestLocation = chest.location.toString()

            if (!openedChests.containsKey(chestLocation)) {
                // 専用チェストを初めて開いた場合
                // ランダムな個数（1～5）の黒色ステンドグラスを配置する
                val slots = Random().nextInt(5) + 1
                for (i in 0 until slots) {
                    event.inventory.setItem(i, ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                }
            }

            val inventory = chest.inventory
            var slot = 0
            val lootTask = object : BukkitRunnable() {
                override fun run() {
                    if (slot < inventory.size) {
                        val item = inventory.getItem(slot)
                        // スロットが空白なら処理を終了
                        if (item == null) {
                            cancel()
                            //openedChests.remove(chestLocation)
                            return
                        }
                        // 黒色板ガラスをアイテムに置き換える
                        if (item.type == Material.BLACK_STAINED_GLASS_PANE) {
                            val setItem = getRandomItem()

                            if (setItem == Material.COAL) {
                                val coal = ItemStack(setItem, 1)
                                val meta: ItemMeta? = coal.itemMeta
                                meta?.setDisplayName("特別な石炭")
                                meta?.setLore(listOf("タスク用品"))
                                meta?.addEnchant(Enchantment.LURE, 1, false) // 光る演出
                                meta?.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                                coal.itemMeta = meta
                                event.inventory.setItem(slot, coal)
                            } else if(setItem == Material.POTION) {
                                val potion = ItemStack(Material.POTION, 1)
                                val meta = potion.itemMeta as PotionMeta
                                meta.addCustomEffect(PotionEffect(PotionEffectType.SPEED, 10 * 20, 1), true)
                                meta.setDisplayName("速度増加のポーション")
                                meta.setColor(Color.AQUA)
                                potion.itemMeta = meta
                                event.inventory.setItem(slot, ItemStack(potion))
                            } else {
                                event.inventory.setItem(slot, ItemStack(setItem))
                            }
                            chest.location.world?.playSound(chest.location, Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f)
                        }
                        slot ++
                    }
                }
            }
            openedChests[chestLocation] = lootTask
            lootTask.runTaskTimer(plugin, 40L, 20L)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        // チェストが閉じられたとき、漁り処理（lootTask）を中断する
        if (event.inventory.holder is Chest) {
            val chest = event.inventory.holder as Chest
            val chestLocation = chest.location.toString()
            openedChests[chestLocation]?.cancel()
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // チェスト内の黒色板ガラスをクリック不可にする
        if (event.currentItem?.type == Material.BLACK_STAINED_GLASS_PANE) {
            event.isCancelled = true
        }
    }

    // チェストの中身
    fun getRandomItem(): Material {
        val randomValue = Random().nextFloat(100F) // 0から99の間のランダムな整数を生成
        return when {
            randomValue < 4.0 -> Material.COAL         // 石炭（タスク用品）4%
            randomValue < 70.0 -> Material.GOLD_INGOT   // 金インゴット66%
            randomValue < 92.0 -> Material.BREAD       // パン22%
            randomValue < 94.5 -> Material.ARROW        // 矢2.5%
            randomValue < 99.99 -> Material.POTION         // ポーション5%
            else -> Material.NETHER_STAR                // ネザースター0.01%
        }
    }
}
