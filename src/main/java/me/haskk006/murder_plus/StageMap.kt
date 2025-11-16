package me.haskk006.murder_plus

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView

class StageMap : Listener {
    fun giveStageMap(player : Player, location: Location) {
        // プレイヤーの現在の位置で地図を作成
        val mapView: MapView = Bukkit.createMap(player.world)
        mapView.isTrackingPosition = true
        mapView.centerX = location.x.toInt()
        mapView.centerZ = location.z.toInt()
        mapView.scale = MapView.Scale.CLOSE
        mapView.renderers.clear()

        // 新しい地図を作成
        val map = ItemStack(Material.FILLED_MAP)
        val mapMeta = map.itemMeta as MapMeta

        mapMeta.addItemFlags(ItemFlag.HIDE_DESTROYS)

        /*
        mapView.addRenderer(object : MapRenderer() {
            override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player) {
                val playerX = player.location.blockX
                val playerZ = player.location.blockZ

                for (x in 0 until 128) {
                    for (z in 0 until 128) {
                        val world = player.world
                        val blockX = playerX + x - 64 // マップの中心をプレイヤーの位置に合わせます
                        val blockZ = playerZ + z - 64 // 同上

                        val block = world.getHighestBlockAt(blockX, blockZ)
                        val material = block.type

                        // 地形の高さに応じて色を設定します
                        val color = when {
                            block.y > 70 -> MapPalette.LIGHT_GREEN // 高い地形
                            block.y > 64 -> MapPalette.DARK_GREEN  // 低い地形
                            else -> MapPalette.BLUE               // 水面
                        }
                        mapCanvas.setPixel(x, z, color)
                    }
                }
            }
        })
         */

        // 地図のメタデータを設定
        mapMeta.mapView = mapView
        map.itemMeta = mapMeta

        // プレイヤーのインベントリの9番目のスロットに地図を設定
        player.inventory.setItem(8, map)
    }
}