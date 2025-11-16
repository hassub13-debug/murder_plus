package me.haskk006.murder_plus
/* ====================================================================
本pluginの著作権はHASにあります。
第三者への無断譲渡、およびソースコードの無断編集を禁止します。

Copyright © 2024 HAS. All rights reserved.
 ====================================================================*/
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    private val loadMapContentsLocations = LoadMapContentsLocations(this)
    private val playerSettings = PlayerSettings(this)
    private val gameDirection = GameDirection(this, playerSettings, loadMapContentsLocations)
    private val lootChest = LootChest(this, gameDirection)

    override fun onEnable() {
        server.pluginManager.registerEvents(loadMapContentsLocations, this)
        loadMapContentsLocations.getSpawnLocations()
        server.pluginManager.registerEvents(gameDirection, this)
        server.pluginManager.registerEvents(playerSettings, this)
        server.pluginManager.registerEvents(lootChest, this)
        server.pluginManager.registerEvents(QuestBook(gameDirection), this)
        server.pluginManager.registerEvents(StageMap(), this)

        getCommand("game_start")?.setExecutor(gameDirection)
    }
}