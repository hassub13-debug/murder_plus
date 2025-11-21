package me.haskk006.murder_plus

import org.bukkit.*
import org.bukkit.block.*
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.FaceAttachable
import org.bukkit.block.data.type.Switch
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class FootSteps(val time: Double, val locations: List<Location>)

class GameDirection(private val plugin: JavaPlugin, private val playerSetting: PlayerSettings, private val loadMapContentsLocations: LoadMapContentsLocations) : Listener, CommandExecutor {
    var stage: String = ""
    private val murderers = mutableListOf<Player>()
    private lateinit var murderNormals: List<Player>
    private val shadows = mutableListOf<Player>()
    private val survivors = mutableListOf<Player>()
    private lateinit var survivorNormals: List<Player>
    private val detectives = mutableListOf<Player>()
    private val shielderes = mutableListOf<Player>()
    private var shield1target: String = ""
    private var shield2target: String = ""
    lateinit var bossBar: BossBar
    private var murderersKilled = 0
    private var survivorsKilled = 0
    private val deathLocations = mutableListOf<Location>()
    var inGameFlag = 0
    var limitationFlag = false
    var task1Activated = false
    var coalAmount = 0
    var startTask2Count = 0
    var levern = 0
    var leverIsOn = 0
    var task2Activated = false
    var matchFinishedFlag = 0
    lateinit var loadMapContents: MutableList<LoadMapContentsLocations.MapContentsLocations>
    var mapContents = mutableListOf<LoadMapContentsLocations.ContentsLocations>()
    val tntChests = mutableMapOf<Location, Boolean>()
    var escapeCountDown = 30.0
    var escapeStart = false
    val openedChests = mutableMapOf<String, BukkitRunnable>()
    private var hasInventoriesBlock = mutableListOf<BlockInfo>()
    data class BlockInfo(val location: Location, val type: Material, val data: BlockData)
    var foot = true
    private var footStepsRecords = mutableListOf<FootSteps>()
    private val METADATA_DEATH = "deathHandled"

        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("game_start", ignoreCase = true) && inGameFlag == 0) {
            stage = ""
            if (args.isNotEmpty()) {
                when (args[0]) {
                    "s1" -> stage = "フンザン村"
                    "s2" -> stage = "コライティランド城"
                    "s3" -> stage = "2025年冬"
                }
            } else {
                return false
            }
            plugin.logger.info("選択したstage: $stage")

            loadMapContents = loadMapContentsLocations.mapContentsLocations
            loadMapContents.forEach { map ->
                if (map.mapName == stage) {
                    mapContents = map.locations
                }
            }
            inGameFlag = 1
            murderers.clear()
            shadows.clear()
            survivors.clear()
            detectives.clear()
            shielderes.clear()
            shield1target = ""
            shield2target = ""
            murderersKilled = 0
            survivorsKilled = 0
            deathLocations.clear()

            task1Activated = false
            coalAmount = 0
            task2Activated = false
            startTask2Count = 0
            levern = 0
            leverIsOn = 0
            escapeStart = false
            escapeCountDown = 30.0

            openedChests.clear()
            hasInventoriesBlock.clear()

            var elapsedTime = 0.0
            val timeOfMoveToWaitingArea = 20.0
            val timeOfMatchStart = 30.0
            matchFinishedFlag = 0
            var timeOfBackToLobby = 10.0

            tntChests.clear()
            foot = true
            footStepsRecords.clear()

            object : BukkitRunnable() {
                override fun run() {
                    if (::bossBar.isInitialized) { bossBar.removeAll() }

                    if (matchFinishedFlag == 0) {
                        // game_startからマップへ移動まで
                        if (elapsedTime <= timeOfMoveToWaitingArea) { // マッチ開始前
                            bossBar = Bukkit.createBossBar("${ChatColor.WHITE}あと${(timeOfMoveToWaitingArea - elapsedTime).toInt()}秒で${ChatColor.GREEN}$stage ${ChatColor.WHITE}に移動します...", BarColor.PURPLE, BarStyle.SOLID)
                            bossBar.progress = ((timeOfMoveToWaitingArea - elapsedTime) / timeOfMoveToWaitingArea).toDouble()
                            Bukkit.getOnlinePlayers().forEach { player ->
                                bossBar.addPlayer(player)
                                // プレイヤーのHP＆満腹度を回復
                                player.health = player.maxHealth
                                player.foodLevel = 20
                            }
                            if ((timeOfMoveToWaitingArea - elapsedTime) < 4) {
                                Bukkit.getOnlinePlayers().forEach { player ->
                                    player.playSound(player.location, Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.1f)
                                }
                            }
                            if (elapsedTime == timeOfMoveToWaitingArea) { // マップへ移動
                                task1Activated = true
                                startMatchSetting() //マッチの初期設定
                            }
                        }
                        // マップへワープから試合開始まで
                        else if (elapsedTime <= timeOfMatchStart) { // 10秒間スポーン地点で待機
                            limitationFlag = true

                            bossBar = Bukkit.createBossBar("ゲーム開始まで ${(timeOfMatchStart - elapsedTime).toInt()}秒", BarColor.PURPLE, BarStyle.SOLID)
                            bossBar.progress = (timeOfMatchStart - elapsedTime) / 10.0
                            Bukkit.getOnlinePlayers().forEach {
                                bossBar.addPlayer(it)
                                if ((timeOfMatchStart - elapsedTime) == 0.0) {
                                    Bukkit.getServer().onlinePlayers.forEach { player ->
                                        player.sendTitle("${ChatColor.WHITE}${ChatColor.BOLD}GAME START!!", "", 10, 70, 20)
                                        player.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f)
                                        player.gameMode = GameMode.SURVIVAL
                                        // task1の座標にマーカーを表示
                                        val task1 = mapContents.find {it.contents == "task1Location"}?.locations?.get(0)
                                        val task1Location = Location(Bukkit.getWorlds()[0], task1?.x!!, task1.y, task1.z)
                                        val shulker = Bukkit.getWorlds()[0].spawnEntity(task1Location, EntityType.SHULKER) as Shulker
                                        shulker.isInvisible = true
                                        shulker.isGlowing = true
                                        shulker.setAI(false)
                                        // task1のUIを表示
                                        val text = "${ChatColor.RED}納品場所"
                                        displayContentUI(task1Location, text)
                                    }
                                    limitationFlag = false
                                } else if ((timeOfMatchStart - elapsedTime) <= 4) {
                                    // マッチ開始残り5秒でカウントダウンのSEを鳴らす
                                    it.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                                }
                            }
                            // マッチ開始後15秒間はプレイヤーを無敵状態にする
                            if (elapsedTime < 45) {
                                Bukkit.getOnlinePlayers().forEach { player ->
                                    player.world.spawnParticle(Particle.VILLAGER_HAPPY, player.location, 20, 0.5, 0.5, 0.5)
                                }
                            }
                        }
                        // マッチ中
                        else {
                            // 足跡を表示
                            footStep(elapsedTime)
                            // マッチ開始後15秒間はプレイヤーを無敵状態にする
                            if (elapsedTime < 45) {
                                Bukkit.getOnlinePlayers().forEach { player ->
                                    player.world.spawnParticle(Particle.VILLAGER_HAPPY, player.location, 20, 0.5, 0.5, 0.5)
                                }
                            }
                            // 無敵状態の解除
                            if (elapsedTime == 45.0) {
                                playerSetting.invincible = false
                                Bukkit.broadcastMessage("システム: 無敵状態を解除しました")
                            }
                            // 脱出タスク1
                            if (task1Activated) {
                                // 脱出タスク1進行中
                                bossBar = Bukkit.createBossBar("${ChatColor.WHITE}脱出タスク1: ${ChatColor.RED}”特別な石炭”${ChatColor.WHITE}を5個、納品場所に納品する", BarColor.PURPLE, BarStyle.SOLID)
                                bossBar.progress = 0.0
                                Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
                            } else {
                                // 脱出タスク1終了時
                                startTask2Count ++
                                // 脱出タスク2
                                if (startTask2Count == 30) {
                                    task2Activated = true
                                    // プレイヤー人数に応じてレバー（Task2）の数を設定
                                    val playerCount = Bukkit.getOnlinePlayers().size
                                    levern = if (playerCount < 9) 3 else 4
                                    val sSLocations = mapContents.find { it.contents == "switchSpawnLocations" }?.locations?.shuffled()?.take(levern)
                                    sSLocations?.forEach {loc ->
                                        // レバーを配置
                                        val spawnLocation = Location(Bukkit.getWorlds()[0], loc.x, loc.y+1, loc.z)
                                        val leverPlace = Bukkit.getWorlds()[0].getBlockAt(spawnLocation)
                                        leverPlace.type = Material.LEVER
                                        val leverData = leverPlace.blockData as Switch
                                        leverData.attachedFace = FaceAttachable.AttachedFace.FLOOR
                                        leverPlace.blockData = leverData
                                        // task2発生地点にマーカーを表示
                                        val markerLocation = Location(Bukkit.getWorlds()[0], loc.x, loc.y, loc.z)
                                        val shulker = Bukkit.getWorlds()[0].spawnEntity(markerLocation, EntityType.SHULKER) as Shulker
                                        shulker.isInvisible = true
                                        shulker.isGlowing = true
                                        shulker.setAI(false)
                                    }
                                    Bukkit.getOnlinePlayers().forEach { player ->
                                        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)
                                        val task2MainMessage = "${ChatColor.BLUE}${ChatColor.BOLD}脱出タスク2が発生!!"
                                        val task2SubMessage =  "${ChatColor.WHITE}${levern}か所にあるレバーを引き、脱出地点を開放しよう"
                                        player.sendTitle(task2MainMessage, task2SubMessage, 10, 70, 20)
                                    }

                                }
                                if (task2Activated) {
                                    bossBar = Bukkit.createBossBar("${ChatColor.WHITE}脱出タスク2: ${levern}か所にある${ChatColor.LIGHT_PURPLE}”レバー”${ChatColor.WHITE}をONにする", BarColor.PURPLE, BarStyle.SOLID)
                                    bossBar.progress = 0.0
                                    Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
                                }
                            }
                            // 脱出地点解放
                            if (leverIsOn >= 3 && !task2Activated && !escapeStart) {
                                bossBar = Bukkit.createBossBar("${ChatColor.RED}脱出地点${ChatColor.WHITE}に向かう", BarColor.PURPLE, BarStyle.SOLID)
                                bossBar.progress = 0.0
                                Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
                            }
                            // 脱出エリアの処理
                            if (escapeStart) {
                                if (escapeCountDown == 30.0) {
                                    Bukkit.getOnlinePlayers().forEach {
                                        it.playSound(it.location, Sound.ITEM_GOAT_HORN_SOUND_1, 1.0f, 1.0f)
                                    }
                                }
                                if (escapeCountDown > 0) {
                                    bossBar = Bukkit.createBossBar("脱出可能になるまで あと${escapeCountDown.toInt()}秒", BarColor.PURPLE, BarStyle.SOLID)
                                    bossBar.progress = escapeCountDown / 30.0
                                    Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }

                                    escapeCountDown--
                                } else {
                                    bossBar = Bukkit.createBossBar("脱出可能", BarColor.GREEN, BarStyle.SOLID)
                                    Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
                                }
                            }
                            // マッチ終了条件
                            if (murderersKilled >= murderers.size) {
                                handleEndGame(0)
                            } else if (survivorsKilled >= survivors.size) { //プラグインテスト
                                handleEndGame(1)
                            }
                        }
                    }
                    else if (matchFinishedFlag == 1) {
                        bossBar = Bukkit.createBossBar("あと${timeOfBackToLobby.toInt()}秒でロビーに帰還します...", BarColor.PURPLE, BarStyle.SOLID)
                        bossBar.progress = timeOfBackToLobby / 10.0
                        Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }

                        if (timeOfBackToLobby == 0.0) { // 0秒時の処理
                            val lobbyLocation = Location(Bukkit.getWorlds()[0], 0.0, -1.0, 0.0)
                            Bukkit.getServer().onlinePlayers.forEach { player ->
                                clearPlayersInventory(player) // インベントリ初期化


                                player.sendMessage("${ChatColor.RED}timeOfBackToLobby: check1") //test

                                player.teleport(lobbyLocation) // ロビーに転送
                                player.setBedSpawnLocation(lobbyLocation, true) // リスポーン位置を設定
                                player.gameMode = GameMode.ADVENTURE // アドベンチャーモードに設定
                                // ガイドブックを与える
                                val questBook = QuestBook(this@GameDirection)
                                questBook.giveQuestBook(player)
                                // 新しくステージ用のマップを付与
                                val stageMap = StageMap()
                                stageMap.giveStageMap(player, lobbyLocation)
                                // 移動速度をリセット
                                player.walkSpeed = 0.2f
                            }
                            // マーカー（シュルカー）を削除
                            Bukkit.getWorlds()[0]?.entities?.filter { it is Shulker }?.forEach { it.remove() }
                            // トレーダーを削除
                            val traderSpawnLocations = mapContents.find {it.contents == "traderSpawnLocations"}?.locations
                            if (traderSpawnLocations != null) {
                                for (loc in traderSpawnLocations) {
                                    val traderLocation = Location(Bukkit.getWorlds()[0], loc.x, loc.y, loc.z)
                                    traderLocation.world?.getNearbyEntities(traderLocation, 2.0, 2.0, 2.0)?.forEach { entity ->
                                        if (entity.type == EntityType.VILLAGER) {
                                            entity.remove()
                                        }
                                        if (entity.type == EntityType.ARMOR_STAND) {
                                            entity.remove()
                                        }
                                    }
                                }
                            }
                            // インベントリを持つブロックを消去＆再配置
                            hasInventoriesBlock.forEach { blockInfo ->
                                blockInfo.location.block.type = Material.AIR
                                blockInfo.location.block.setType(blockInfo.type, false)
                                blockInfo.location.block.blockData = blockInfo.data
                            }
                            hasInventoriesBlock.clear()
                            // マップ上に生成されたチェスト、脱出タスクを削除
                            for (contentsLocations in mapContents) {
                                val blockLocations = contentsLocations.locations
                                blockLocations.forEach { loc ->
                                    val blockLocation = Location(Bukkit.getWorlds()[0], loc.x, loc.y+1, loc.z)
                                    if (blockLocation.block.type == Material.CHEST) {
                                        // 生成されたすべてのチェストを削除
                                        blockLocation.block.type = Material.AIR
                                    } else if (blockLocation.block.type == Material.LEVER) {
                                        // 生成されたすべてのレバーを削除
                                        blockLocation.block.type = Material.AIR
                                    }
                                }
                            }
                            // プレイヤー死亡地点にあるすべてのPLAYER_HEADを削除
                            for (loc in deathLocations) {
                                val entities = loc.world?.getNearbyEntities(loc, 1.0, 1.0, 1.0)
                                entities?.filter { it is ArmorStand }?.forEach { it.remove() }
                            }
                            // ワールド内にドロップしたアイテムをすべて削除
                            Bukkit.getWorlds()[0].entities.filterIsInstance<Item>().forEach { it.remove() }
                            bossBar.removeAll()
                            cancel()
                        }
                        timeOfBackToLobby --
                    }
                    elapsedTime ++
                }
            }.runTaskTimer(plugin, 0L, 20L)

            return true
        }
        return false
    }

    // マッチ開始時の処理
    private fun startMatchSetting() {
        val players = Bukkit.getOnlinePlayers().shuffled()
        // マップコンテンツ（チェスト、脱出タスクなど）を配置
        setFieldMapContents(players)

        Bukkit.getServer().onlinePlayers.forEach { player ->
            player.gameMode = GameMode.SURVIVAL

            if (player.hasMetadata(METADATA_DEATH)) {
                player.removeMetadata(METADATA_DEATH, plugin)
            }
            // すでにマップを持っている場合、そのマップを削除
            val inventory = player.inventory
            val itemInSlot = inventory.getItem(8)
            if (itemInSlot != null && itemInSlot.type == Material.MAP) {
                inventory.setItem(8, ItemStack(Material.AIR))
            }

            /*
            // 新しくステージ用のマップを付与
            val stageMap = StageMap()
            val mC = mapContents.find{it.contents=="mapCenterLocations"}?.locations!!
            val stageLocation = Location(Bukkit.getWorlds()[0], mC[0].x, mC[0].y, mC[0].z)
            stageMap.giveStageMap(player, stageLocation)
            */

            // お互いのプレイヤーのネームタグを見れないようにする
            val scoreboard: Scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
            val team: Team = scoreboard.getTeam("HideNameTag") ?: scoreboard.registerNewTeam("HideNameTag")
            team.addEntry(player.name)
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
            player.scoreboard = scoreboard
        }

        // プレイヤーの各陣営への振り分け
        // マーダー
        val murderCount = kotlin.math.ceil(players.size * 0.125).toInt()
        murderers.addAll(players.subList(0, murderCount))
        var murderNormals: List<Player> = murderers.toList()

        if (murderers.size > 1) {
            // シャドウ
            val shadowsIndex = Random.nextInt(murderers.size)
            shadows.add(murderers[shadowsIndex])
            murderNormals = murderers.filter { it != murderers[shadowsIndex] }
        }

        // サバイバー
        survivors.addAll(players.subList(murderCount, players.size))
        var survivorNormals: List<Player> = survivors.toList()


        if (survivors.size > 1) {
            // シールダー
            val shielderesIndex = Random.nextInt(survivors.size)
            shielderes.add(survivors[shielderesIndex])
            survivorNormals = survivors.filter { it != survivors[shielderesIndex] }
            if (survivors.size > 5) {
                // 探偵
                val detectiveIndex = Random.nextInt(survivorNormals.size)
                detectives.add(survivors[detectiveIndex])
                survivorNormals = survivors.filter { it != survivors[detectiveIndex] }
            }
        }

        // 役職の振り分け
        murderNormals.forEach {
            var murderersList = murderers.joinToString(separator = " "){ it.name }
            it.sendTitle("${ChatColor.RED}${ChatColor.BOLD}あなたは マーダーです ", "", 10, 70, 20)
            it.sendMessage("${ChatColor.RED}マーダーのプレイヤー：$murderersList")
            giveItem(it, Material.TRIDENT, amount = 1, slot = 0)
            giveItem(it, Material.TNT, amount = 1, slot = 1)
            val speed = it.walkSpeed * 1.2f
            it.walkSpeed = speed
        }

        shadows.forEach {
            var murderersList = murderers.joinToString(separator = " "){ it.name }
            it.sendTitle("${ChatColor.RED}${ChatColor.BOLD}あなたは シャドウ です", "", 10, 70, 20)
            it.sendMessage("${ChatColor.RED}マーダーのプレイヤー：$murderersList")
            giveItem(it, Material.TRIDENT, amount = 1, slot = 0)
            giveItem(it, Material.INK_SAC, amount = 1, slot = 1)
            val speed = it.walkSpeed * 1.2f
            it.walkSpeed = speed
        }

        detectives.forEach {
            it.sendTitle("${ChatColor.BLUE}${ChatColor.BOLD}あなたは 探偵 です", "", 10, 70, 20)
            giveItem(it, Material.BOW, amount = 1, slot = 0) // 探偵に弓を与える
            giveItem(it, Material.ARROW, amount = 3, slot = 1)
        }

        shielderes.forEach {
            it.sendTitle("${ChatColor.BLUE}${ChatColor.BOLD}あなたは シールダー です", "", 10, 70, 20)
            giveItem(it, Material.SHIELD, amount = 1, slot = 0)
            giveItem(it, Material.TOTEM_OF_UNDYING, amount = 1, slot = 1)
        }

        survivorNormals.forEach {
            it.sendTitle("${ChatColor.GREEN}${ChatColor.BOLD}あなたは サバイバー です", "", 10, 70, 20)
        }

    }

    // 試合開始時に特定プレイヤーにアイテムを与える
    private fun giveItem(player: Player, material: Material, amount: Int = 1, slot: Int) {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta
        if (meta != null) {
            if (material == Material.TNT) {
                meta.setDisplayName("スキル：サボタージュ")
                meta.isUnbreakable = true
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            else if (material == Material.INK_SAC) {
                meta.setDisplayName("スキル：煙幕")
                meta.isUnbreakable = true
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            else if (material == Material.BARRIER) {
                meta.setDisplayName("使用できないアイテムスロット")
                meta.isUnbreakable = true
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            item.itemMeta = meta
            player.inventory.setItem(slot, item)
        }
    }

    // マッチ終了時の処理
    private fun handleEndGame(winflag: Int?) {
        matchFinishedFlag = 1
        inGameFlag = 0
        escapeCountDown = 30.0
        val winMessage = when (winflag) {
            0 -> "${ChatColor.GREEN}${ChatColor.BOLD}サバイバーの勝利"
            1 -> "${ChatColor.RED}${ChatColor.BOLD}マーダーの勝利"
            else -> "${ChatColor.BOLD}引き分け(Exception)"
        }
        Bukkit.getOnlinePlayers().forEach { player ->
            // 勝利時のタイトルコール＆SE
            player.sendTitle(winMessage, "", 10, 70, 20)
            player.playSound(player.location, Sound.ENTITY_WITHER_SPAWN, 0.9f, 1.1f)
            // scoreboardの削除
            val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
            var team = scoreboard?.getTeam("HideNameTag")
            team?.removeEntry(player.name)
        }
        // 無敵状態の付与
        playerSetting.invincible = true
        Bukkit.broadcastMessage("システム: 無敵状態を付与しました")
    }

    // プレイヤーのインベントリを初期化
    private fun clearPlayersInventory(player: Player) {
        val inventory = player.inventory
        for (itemStack in inventory.contents) {
            if (itemStack != null && itemStack.type != Material.WRITTEN_BOOK) {
                inventory.remove(itemStack)
            }
        }
        player.inventory.setItemInOffHand(ItemStack(Material.AIR))
    }

    private fun footStep(elapsedTime: Double) {
        // 足跡の表示を60秒間に制限
        val footStepsList = mutableListOf<Location>()
        if (footStepsRecords.size > 60) {
            footStepsRecords.removeAt(0)
        }
        for (p in Bukkit.getOnlinePlayers()) {
            // マーダーにのみ足跡が見えるようにする
            if (murderers.contains(p)) {
                footStepsRecords.forEach { element ->
                    element.locations.forEach { loc ->
                        p.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, loc, 1)
                    }
                }
            }
            // 足跡の発生位置を記録
            if (!murderers.contains(p) && p.gameMode == GameMode.SURVIVAL) {
                val l = p.location
                l.y = floor(l.y)
                if (!l.clone().subtract(0.0, 0.95, 0.0).block.isEmpty) {
                    val x =cos(Math.toRadians(p.location.yaw.toDouble())) * 0.25
                    val z = sin(Math.toRadians(p.location.yaw.toDouble())) * 0.25
                    if (foot) {
                        l.add(x, 0.025, z)
                    } else {
                        l.subtract(x, -0.025, z)
                    }
                    footStepsList.add(l)
                }
            }
        }
        foot = false
        footStepsRecords.add(FootSteps(elapsedTime, footStepsList))
    }

    // プレイヤー死亡時の処理
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val deathLocation = player.location
        event.deathMessage = "" // 死亡ログを非表示にする
        // 不死のトーテムは、特定の死因（落下など）では発動しないようにする
        val damageCause = player.lastDamageCause?.cause
        if (damageCause == EntityDamageEvent.DamageCause.FALL) {
            if (player.inventory.contains(ItemStack(Material.TOTEM_OF_UNDYING))) {
                player.inventory.removeItem(ItemStack(Material.TOTEM_OF_UNDYING))
            }
        }
        // プレイヤーが死亡した処理の二重カウント対策　※要改善
        if (player.hasMetadata(METADATA_DEATH)) { return }
        player.setMetadata(METADATA_DEATH, FixedMetadataValue(plugin, true))

        // 特殊アイテム（ガイドブック、マップ、トライデント、弓、TNT、トリップワイヤーフック）をドロップ（喪失）しないようにする
        val itemsToKeep = setOf(Material.WRITTEN_BOOK, Material.FILLED_MAP, Material.TRIDENT, Material.BOW, Material.TNT, Material.TRIPWIRE_HOOK, Material.BARRIER, Material.INK_SAC, Material.TOTEM_OF_UNDYING)
        val keptItems = event.drops.filter { it.type in itemsToKeep }
        event.drops.removeIf { it.type in itemsToKeep }
        event.entity.player?.inventory?.addItem(*keptItems.toTypedArray())
        // プレイヤーのゲームモードをスペクテイターモードに設定
        player.gameMode = GameMode.SPECTATOR
        // 死亡したプレイヤーが所属するチームのKilledカウントを1増やす
        if (murderers.contains(player)) {
            murderersKilled++
        } else if (survivors.contains(player)) {
            survivorsKilled++
        }
        // プレイヤーの死亡座標に死体を残す
        var block = player.location.block
        val corpseLocation = block.location.clone().subtract(0.0, 1.5, 0.0)
        val corpse = block.location.world?.spawn(corpseLocation, ArmorStand::class.java)
        corpse?.apply {
            isVisible = false // 透明にする
            isInvulnerable = true // 破壊不可能にする
            setGravity(false) // 重力を無効にする
            isCustomNameVisible = false
            isMarker = true
        }
        val skull = ItemStack(Material.PLAYER_HEAD, 1)
        val meta = skull.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(player.uniqueId)
        skull.itemMeta = meta
        corpse?.setHelmet(skull)

        // プレイヤーの死亡地点を記録
        if (corpse != null) {
            deathLocations.add(corpse.location)
            // 死体にパーティクルを発生させる
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                if (deathLocations.contains(corpse.location)) {
                    player.world.spawnParticle(Particle.REDSTONE, block.location, 30, 2.0, 1.0, 2.0, Particle.DustOptions(Color.RED, 2.0f))
                }
            }, 0L, 20L)
            // リスポーン地点の設定
            var respawn = block
            if (block.type != Material.AIR) {
                val airBlocks = mutableListOf<Block>()
                for (y in 0..2) {
                    for (x in -1..1) {
                        for (z in -1..1) {
                            val relativeBlock = block.getRelative(x, y, z)
                            if (relativeBlock.type == Material.AIR) {
                                airBlocks.add(relativeBlock)
                            }
                        }
                    }
                    if (airBlocks.isNotEmpty()) {
                        respawn = airBlocks[Random.nextInt(airBlocks.size)]
                        break
                    }
                }
            }
            else if (block.type == Material.AIR) {
                var timeout = 0
                while (block.type == Material.AIR) {
                    block = block.getRelative(0, -1, 0)
                    timeout ++
                    if (timeout == 10) { break } // 最高10ブロックの高さまでカウント
                }
                respawn = block.getRelative(0, 1, 0)
            }
            player.setBedSpawnLocation(respawn.location, true) // プレイヤーのリスポーン地点を死亡した座標に設定
        }
    }

    // プレイヤーが他のプレイヤーから攻撃されると即死する
    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager
        val killer = event.damageSource.causingEntity as Player
        if (victim is Player && !playerSetting.invincible) {
            if (murderers.contains(killer) && murderers.contains(victim)) {
                if (killer != victim) {
                    event.isCancelled = true
                }
            }
            else if (attacker is Arrow || attacker is Trident) {
                // シールドを付与されたプレイヤーが攻撃された時の処理
                if (victim.name == shield1target || victim.name == shield2target) {
                    if (victim.name == shield1target) { shield1target = "None" }
                    if (victim.name == shield2target) { shield2target = "None" }
                    victim.playEffect(EntityEffect.TOTEM_RESURRECT)
                    victim.playSound(victim.location, Sound.ITEM_TOTEM_USE, 0.6f, 1.0f)
                    victim.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
                    victim.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 0))
                    victim.sendMessage("シールドがあなたを守った")
                    event.isCancelled = true
                }
                else if (victim.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    event.isCancelled = true
                }
                else if (survivors.contains(killer) && survivors.contains(victim)) {
                    killer.setHealth(0.0)
                    victim.setHealth(0.0)
                }
                else {
                    victim.setHealth(0.0)
                }
            }
            else if (attacker is Player) {
                val itemInHand = attacker.inventory.itemInMainHand
                if (itemInHand.type == Material.TRIDENT) {
                    if (victim.name == shield1target || victim.name == shield2target) {
                        if (victim.name == shield1target) { shield1target = "None" }
                        if (victim.name == shield2target) { shield2target = "None" }
                        victim.playEffect(EntityEffect.TOTEM_RESURRECT)
                        victim.playSound(victim.location, Sound.ITEM_TOTEM_USE, 0.6f, 1.0f)
                        victim.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
                        victim.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 0))
                        victim.sendMessage("シールドがあなたを守った")
                        event.isCancelled = true
                    }
                    else if (victim.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                        event.isCancelled = true
                    }
                    else {
                        victim.setHealth(0.0)
                    }
                }
            }
        }
        else if (event.entity.type == EntityType.PAINTING) {
            event.isCancelled = true
        }
    }

    // プレイヤーの途中抜け処理
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (murderers.contains(player)) {
            murderersKilled++
        } else if (survivors.contains(player)) {
            survivorsKilled++
        }
        val team = Bukkit.getScoreboardManager()?.mainScoreboard?.getTeam("HideNameTag")
        team?.removeEntry(player.name)
    }

    @EventHandler
    fun limitationAction(event: PlayerMoveEvent) {
        val player = event.player
        // マッチ中以外の時、体力と満腹度を最大にする
        if (inGameFlag == 0) {
            // 体力と満腹度を最大まで回復
            player.health = player.maxHealth
            player.foodLevel = 20
            player.saturation = 20f
        }
        // プレイヤーを一定時間移動不可能にする処理
        if (limitationFlag) {
            val setLocation = Location(
                event.to?.world,
                event.from.x,
                event.from.y,
                event.from.z,
                event.to?.yaw!!,
                event.to?.pitch!!
            )
            event.setTo(setLocation);
        }
        // 脱出地点の処理
        if (inGameFlag == 1 && leverIsOn >= 3 && !task2Activated) {
            val eA = mapContents.find {it.contents=="escapeArea"}?.locations
            val xMin = eA!![0].x
            val xMax = eA[1].x
            val yMin = eA[0].y
            val yMax = eA[1].y
            val zMin = eA[0].z
            val zMax = eA[1].z
            val loc = player.location
            if (survivors.contains(player)) {
                if (player.gameMode != GameMode.SPECTATOR) {
                    if (loc.x in xMin..xMax && loc.y in yMin..yMax && loc.z in zMin..zMax) {
                        if (matchFinishedFlag == 0) {
                            escapeStart = true
                        }
                    }
                    if (escapeCountDown <= 0 && (loc.x in xMin..xMax && loc.y in yMin..yMax && loc.z in zMin..zMax)) {
                        handleEndGame(0)
                    }
                }
            }
        } else {
            return
        }
    }

    // プレイヤー、チェスト、脱出タスクのスポーン地点を設定
    private fun setFieldMapContents(players: List<Player>) {
        players.forEach {
            it.sendMessage("setFieldMapContents: test") //test
        }

        val pSLocations = mapContents.find { it.contents == "playerSpawnLocations" }?.locations?.shuffled()

        if (pSLocations != null) {
            // リスト内の各 Location オブジェクトを「(X, Y, Z)」形式の文字列に変換し、改行で結合
            val locationMessage = pSLocations.joinToString("\n") { location ->
                // 座標を見やすくするために整数に丸めます
                val x = location.x.roundToInt()
                val y = location.y.roundToInt()
                val z = location.z.roundToInt()

                // 座標文字列を整形
                "§e- §aワールド: §6[§cX: $x, §aY: $y, §9Z: $z§6]\n"
            }
            players.forEach {
                it.sendMessage(locationMessage) //test
            }
        }


        players.forEachIndexed { index, player ->
            if (pSLocations?.get(index) is LoadMapContentsLocations.LocationXYZ) {
                val spawnPloc = pSLocations[index]
                val loc = Location(Bukkit.getWorlds()[0], spawnPloc.x, spawnPloc.y+1, spawnPloc.z)

                val x = loc.x.roundToInt()
                val y = loc.y.roundToInt()
                val z = loc.z.roundToInt()

                player.teleport(loc)
                player.sendMessage("${ChatColor.RED}§6[§cX: $x, §aY: $y, §9Z: $z§6]") //test
            }
        }

        val cSLocations = mapContents.find { it.contents == "chestSpawnLocations" }?.locations?.shuffled()?.take(100)
        cSLocations?.forEach { loc ->
            val spawnCloc = Location(Bukkit.getWorlds()[0], loc.x, loc.y+1, loc.z)
            val chestPlace = Bukkit.getWorlds()[0].getBlockAt(spawnCloc)
            chestPlace.type = Material.CHEST
        }

        //トレーダー
        val tSLocations = mapContents.find {it.contents == "traderSpawnLocations"}?.locations
        if (tSLocations != null) {
            for (loc in tSLocations) {
                val spawnLocation = Location(Bukkit.getWorlds()[0], loc.x, loc.y, loc.z, loc.yaw.toFloat(), 0.0f)
                val villager = Bukkit.getWorld("world")?.spawnEntity(spawnLocation, EntityType.VILLAGER) as Villager
                villager.isInvulnerable = true
                villager.setAI(false)
                villager.isCustomNameVisible = true // カスタムネームを表示する
                villager.customName = "${ChatColor.WHITE}${ChatColor.BOLD}アイテムトレーダー"

                val task1KeyItem = MerchantRecipe(getTask1KeyItem(ItemStack(Material.COAL)), 99999)
                task1KeyItem.addIngredient(ItemStack(Material.GOLD_INGOT, 20))
                val bow = MerchantRecipe(ItemStack(Material.BOW), 99999)
                bow.addIngredient(ItemStack(Material.GOLD_INGOT, 20))
                val arrow = MerchantRecipe(ItemStack(Material.ARROW), 99999)
                arrow.addIngredient(ItemStack(Material.GOLD_INGOT, 10))
                val pie = MerchantRecipe(ItemStack(Material.PUMPKIN_PIE), 99999)
                pie.addIngredient(ItemStack(Material.GOLD_INGOT, 5))
                val gold = MerchantRecipe(ItemStack(Material.GOLD_INGOT, 5), 99999)
                gold.addIngredient(getTask1KeyItem(ItemStack(Material.COAL)))
                villager.recipes = listOf(task1KeyItem, bow, arrow, pie, gold)
            }
        }
    }

    fun getTask1KeyItem(setItem: ItemStack): ItemStack {
        val meta: ItemMeta? = setItem.itemMeta
        meta?.setDisplayName("特別な石炭")
        meta?.setLore(listOf("タスク用品"))
        meta?.addEnchant(Enchantment.LURE, 1, false) // 光る演出
        meta?.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        setItem.itemMeta = meta
        return setItem
    }

    fun displayContentUI(location: Location, text: String) {
        // アーマースタンドを作成してネームタグを表示
        val entities = location.world?.getNearbyEntities(location, 1.0, 1.0, 1.0)
        if (entities?.any { it is ArmorStand } == false) {
            val adjustedLocation = location.add(0.5, 0.0, 0.5)
            val armorStand = location.world?.spawn(adjustedLocation, ArmorStand::class.java)
            armorStand?.isVisible = false // アーマースタンドを非表示にする
            armorStand?.isCustomNameVisible = true // カスタムネームを表示する
            armorStand?.customName = text
            plugin.logger.info("armorstand: $location")
        }
    }

    // 脱出タスク1（アイテム納品）
    @EventHandler
    fun onInventoryClick(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val holder = inventory.holder
        coalAmount = 0
        if (holder is Barrel && task1Activated) { // holder.customName == "脱出タスク１"
            val task1loc = mapContents.find {it.contents=="task1Location"}?.locations
            val task1location = Location(Bukkit.getWorlds()[0], task1loc!![0].x, task1loc[0].y, task1loc[0].z)
            if (holder.location == task1location) {
                val specialCoal = ItemStack(Material.COAL)
                val meta = specialCoal.itemMeta
                meta?.setDisplayName("特別な石炭")
                specialCoal.itemMeta = meta

                inventory.contents.filterNotNull().forEach { coal ->
                    if (coal.type == Material.COAL && coal.itemMeta?.hasDisplayName() == true) {
                        coalAmount += coal.amount
                    }
                }
                if (coalAmount >= 5) {
                    holder.inventory.clear()
                    task1Activated = false
                    // マーカー（スライム）を削除
                    val world = Bukkit.getWorld("world")
                    world?.entities?.filter { it is Shulker }?.forEach { it.remove() }

                    Bukkit.getServer().onlinePlayers.forEach { player ->
                        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)
                        player.sendMessage("システム: 脱出タスク１を完了しました！")
                    }
                }
            }
        }
    }
    @EventHandler
    fun onInventoryClick(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return
        // マッチ中にインタラクト不可能にする対象
        if (inGameFlag == 1) {
            if (block.type == Material.ENDER_CHEST || block.type == Material.CAKE || block.type == Material.ITEM_FRAME || block.type == Material.LECTERN) {
                event.isCancelled = true
            } else if ((block.type.name.contains("SIGN") || block.type.name.contains("TRAPDOOR") || block.type.name.contains("CANDLE"))) {
                event.isCancelled = true
            }
            // 植木鉢から植物を取れないようにする
            if (event.action == Action.RIGHT_CLICK_BLOCK) {
                if (block.type == Material.FLOWER_POT || block.type.name.contains("POTTED") || block.type == Material.ITEM_FRAME || block.type == Material.DECORATED_POT || block.type == Material.LECTERN) {
                    event.isCancelled = true
                }
            }
        }
        // 観戦中のプレイヤーはインタラクト操作を不可能にする
        if (player.gameMode == GameMode.SPECTATOR) {
            event.isCancelled = true
        }
        // 脱出タスク1が完了すると、樽を開けないようにする
        if (block.state is Barrel) {
            val task1loc = mapContents.find {it.contents=="task1Location"}?.locations
            val task1location = Location(Bukkit.getWorlds()[0], task1loc!![0].x, task1loc[0].y, task1loc[0].z)
            if (!task1Activated && block.location == task1location) {
                event.isCancelled = true // インタラクト不可能にする
            }
        }
    }

    // 脱出タスク2（スイッチ作動）
    @EventHandler
    fun onLeverInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            if (event.player.gameMode == GameMode.SURVIVAL || event.player.gameMode == GameMode.CREATIVE) {
                val block = event.clickedBlock ?: return
                val lever = block.blockData as? Switch ?: return

                if (block.type == Material.LEVER && lever.isPowered) {
                    event.isCancelled = true
                }
                if (task2Activated) {
                    if (block.type == Material.LEVER && !lever.isPowered) {
                        leverIsOn += 1
                        // マーカー（スライム）を削除
                        val entities = block.location.world?.getNearbyEntities(block.location, 1.0, 1.0, 1.0)
                        entities?.filter { it is Shulker }?.forEach { it.remove() }
                        if (leverIsOn < levern) {
                            // レバーが惹かれたことをプレイヤーに通知
                            Bukkit.getServer().onlinePlayers.forEach { player ->
                                player.sendMessage("システム: レバーが引かれました")
                            }
                        } else if (leverIsOn == levern) {
                            task2Activated = false

                            Bukkit.getServer().onlinePlayers.forEach { player ->
                                player.sendMessage("システム: 脱出タスク２を完了しました！")
                                player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)

                                val escapeMainMessage = "${ChatColor.YELLOW}${ChatColor.BOLD}脱出地点解放!!"
                                val escapeSubMessage =  "${ChatColor.WHITE}脱出地点に向かおう!"
                                player.sendTitle(escapeMainMessage, escapeSubMessage, 10, 70, 20)
                            }
                        }
                    }
                }
            }
        }
    }

    // インタラクトに関する処理（看板クリックによるゲーム開始、ブービートラップの処理など）
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        // 看板をインタラクトしてマッチを開始
        val clickedBlock = event.clickedBlock?.state
        if (clickedBlock is Sign) {
            if (player.name == "HAS_and_Coffee" || player.name == "nekoguma366") {
                val lines = clickedBlock.lines
                when (lines[2]) {
                    "フンザン村" -> player.performCommand("game_start s1")
                    "コライティランド城" -> player.performCommand("game_start s2")
                    "スノーウィル村" -> player.performCommand("game_start s3")
                }
            }
        }

        // サボタージュ
        if (event.action == Action.RIGHT_CLICK_BLOCK && player.inventory.itemInMainHand.type == Material.TNT) {
            val block = event.clickedBlock
            if (block != null && block.type == Material.CHEST && murderers.contains(player)) {
                if (block.state is Chest) {
                    val chest = block.state as Chest
                    if (tntChests[chest.location] != true) {
                        tntChests[chest.location] = true
                        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                            if (tntChests[chest.location] == true) {
                                if (murderers.contains(player)) {
                                    player.spawnParticle(Particle.REDSTONE, chest.location.add(0.5, 1.0, 0.5), 10, Particle.DustOptions(Color.RED, 1.0f))
                                }
                            }
                        }, 0L, 20L)
                        // スキルのクールタイムを設定
                        giveItem(player, Material.BARRIER, amount = 1, slot = 1)
                        if (inGameFlag == 1) {
                            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                                giveItem(player, Material.TNT, amount = 1, slot = 1)
                            }, 200L)
                            event.isCancelled = true
                            player.playSound(player.location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9f, 1.0f)
                            player.sendMessage("システム: 爆弾をセットしました。10秒後に再使用可能")
                        }
                    }
                }
            }
        }
        // 煙幕
        if (player.inventory.itemInMainHand.type == Material.INK_SAC && event.action.toString().contains("RIGHT_CLICK")) {
            if (inGameFlag == 1) {
                for (entity in player.world.getNearbyEntities(player.location, 30.0, 30.0, 30.0)) {
                    if (entity is Player && !murderers.contains(entity)) {
                        entity.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 200, 1))
                    }
                }
                // スキルのクールタイムを設定
                giveItem(player, Material.BARRIER, amount = 1, slot = 1)
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    giveItem(player, Material.INK_SAC, amount = 1, slot = 1)
                }, 600L)
                player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 120, 4))
                player.world.spawnParticle(Particle.SQUID_INK, player.location, 30, 2.0, 1.0, 2.0)
                event.isCancelled = true
                player.playSound(player.location, Sound.ITEM_INK_SAC_USE, 0.9f, 1.0f)
                player.sendMessage("システム: 煙幕を使用しました。30秒後に再使用可能")
            }
        }
        // シールド（トーテム）
        if (player.inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING && event.action.toString().contains("RIGHT_CLICK")) {
            if (inGameFlag == 1) {
                if (shield2target == "") {
                    val otherPlayers = survivors.filter { it != player && it.name != shield1target && it.gameMode == GameMode.SURVIVAL}
                    if (otherPlayers.isNotEmpty()) {
                        val randomPlayer = otherPlayers.random()
                        randomPlayer.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Int.MAX_VALUE, 0, false, false))
                        shield2target = randomPlayer.name
                        player.sendMessage("システム: プレイヤーにシールドを付与しました")
                    }
                }
            }
        }
    }
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val rightClicked = event.rightClicked

        // プレイヤーがシールドを持っているかを確認
        if (player.inventory.itemInMainHand.type == Material.SHIELD && shielderes.contains(player)) {
            // シールダーがはじめて触れる相手にシールドを付与する
            if (rightClicked is Player && shield1target == "") {
                if (rightClicked.gameMode == GameMode.SURVIVAL) {
                    if (rightClicked.name == shield2target) {
                        player.sendMessage("そのプレイヤーにはすでにシールドが付与されています")
                    }
                    else {
                        shield1target = rightClicked.name
                        rightClicked.world.spawnParticle(Particle.ELECTRIC_SPARK, rightClicked.location, 10, 1.0, 1.0, 1.0)
                        rightClicked.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Int.MAX_VALUE, 0, false, false))
                        player.sendMessage("プレイヤーにシールドを付与しました")
                    }
                }
            }
        }
    }
    @EventHandler
    fun onChestOpen(event: InventoryOpenEvent) {
        val player = event.player as Player
        val inventory = event.inventory
        // 空けたチェストがブービートラップだった場合、チェストが爆発する
        if (inventory.holder is Chest) {
            val chest = inventory.holder as Chest
            if (tntChests[chest.location] == true && !murderers.contains(player)) {
                chest.location.world?.createExplosion(chest.location, 4.0f, false, false)
                if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && player.health < 16.0) {
                    // シールドが付与されていて、かつ体力が16以下のプレイヤーは、ダメージを無効化する
                    player.playEffect(EntityEffect.TOTEM_RESURRECT)
                    player.playSound(player.location, Sound.ITEM_TOTEM_USE, 0.6f, 1.0f)
                    player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
                    player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 0))
                    player.sendMessage("シールドがあなたを守った")
                    if (player.name == shield1target) { shield1target = "None" }
                    if (player.name == shield2target) { shield2target = "None" }
                }
                else {
                    // ダメージとデバフを与える
                    player.damage(16.0)
                    player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 100, 5))
                }
                tntChests.remove(chest.location)
            }
        }
    }

    // インベントリを含むブロックを記録
    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val block = event.inventory.holder as? BlockState ?: return
        hasInventoriesBlock.add(BlockInfo(block.location, block.type, block.blockData))
    }

    // プレイヤーがサーバーに入室した際の処理
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val inventory = player.inventory
        // ロビーにテレポート
        val lobbyLocation = Location(Bukkit.getWorlds()[0], 0.0, -1.0, 0.0)
        player.teleport(lobbyLocation)
        // アドベンチャーモードに設定
        player.gameMode = GameMode.ADVENTURE
        // 移動速度をリセット
        player.walkSpeed = 0.2f
        // インベントリを初期化
        for (itemStack in inventory.contents) {
            inventory.remove(itemStack)
        }
        // ガイドブックを与える
        val questBook = QuestBook(this)
        questBook.giveQuestBook(player)
        // ステージマップを与える
        val stageMap = StageMap()
        stageMap.giveStageMap(player, lobbyLocation)
        // 経験値を0にリセット
        event.player.level = 0
    }

    // トライデント    // 矢が壁や床に刺さると消失する
    @EventHandler
    fun onTridentHit(event: ProjectileHitEvent) {
        val entity = event.entity
        if (entity is Trident && entity.shooter is Player) {
            val player = entity.shooter as Player
            entity.remove()

            if (matchFinishedFlag == 0) {
                giveItem(player, Material.BARRIER, amount = 1, slot = 0)
                object : BukkitRunnable() {
                    override fun run() {
                        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0f, 1.0f)
                        giveItem(player, Material.TRIDENT, amount = 1, slot = 0)
                    }
                }.runTaskLater(plugin, 100L)
            }
        } else if (entity is Arrow && entity.shooter is Player) {
            entity.remove()
        }
    }
}