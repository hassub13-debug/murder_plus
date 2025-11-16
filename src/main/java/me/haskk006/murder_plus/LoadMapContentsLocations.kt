package me.haskk006.murder_plus

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.json.simple.parser.JSONParser

class LoadMapContentsLocations(private val plugin: JavaPlugin): Listener {
    @Serializable
    data class LocationXYZ(val x: Double, val y: Double, val z: Double, val yaw: Double)
    @Serializable
    data class ContentsLocations(val contents: String, val locations: MutableList<LocationXYZ>)
    @Serializable
    data class MapContentsLocations(val mapName: String, val locations: MutableList<ContentsLocations>)
    @Serializable
    data class Stage(val mapName: String,
                     val mapRange: List<LocationXYZ>,
                     val task1: List<LocationXYZ>,
                     val center: List<LocationXYZ>,
                     val trader: List<LocationXYZ>,
                     val escapeArea: List<LocationXYZ>)

    private val chestSpawnLocations = mutableListOf<LocationXYZ>()
    private val playerSpawnLocations = mutableListOf<LocationXYZ>()
    private val switchSpawnLocations = mutableListOf<LocationXYZ>()
    private val traderSpawnLocations = mutableListOf<LocationXYZ>()
    private val mapCenterLocations = mutableListOf<LocationXYZ>()
    private val escapeArea = mutableListOf<LocationXYZ>()
    private val task1Location = mutableListOf<LocationXYZ>()
    var mapContentsLocations = mutableListOf<MapContentsLocations>()

    private val allMapSettings = """
    [
        {
            "mapName":"フンザン村",
            "mapRange":[
                {"x":-189, "y":48, "z":628, "yaw":0.0},
                {"x":111, "y":110, "z":878, "yaw":0.0}
            ],
            "center":[{"x":-45.0, "y":-1.0, "z":755.0, "yaw":0.0}],
            "task1":[{"x":-22.0, "y":58.0, "z":700.0, "yaw":0.0}],
            "trader":[
                {"x":-143.0, "y":80.0, "z":720.0, "yaw":90.0},
                {"x":-56.0, "y":69.0, "z":770.0, "yaw":0.0},
                {"x":60.0, "y":80.0, "z":741.0, "yaw":-90.0}
            ],
            "escapeArea":[
                {"x":-88, "y":66, "z":639, "yaw":0.0},
                {"x":-79, "y":70, "z":646, "yaw":0.0}
            ]
        },
        {
            "mapName":"コライティランド城",
            "mapRange":[
                {"x":-944, "y":70, "z":722, "yaw":0.0},
                {"x":-695, "y":147, "z":971, "yaw":0.0}
            ],
            "center":[{"x":-815.0, "y":-1.0, "z":843.0, "yaw":0.0}],
            "task1":[{"x":-843.0, "y":105.0, "z":830.0, "yaw":0.0}],
            "trader":[
                {"x":-830.0, "y":76.0, "z":753.0, "yaw":-90.0},
                {"x":-862.0, "y":93.0, "z":879.0, "yaw":-90.0},
                {"x":-909.0, "y":76.0, "z":942.0, "yaw":90.0},
                {"x":-715.0, "y":76.0, "z":895.0, "yaw":180.0}
            ],
            "escapeArea":[
                {"x":-820, "y":74, "z":731, "yaw":0.0},
                {"x":-810, "y":78, "z":735, "yaw":0.0} 
            ]
        },
        {
            "mapName":"2025年冬",
            "mapRange":[
                {"x":-176, "y":47, "z":1327, "yaw":0.0},
                {"x":173, "y":130, "z":1647, "yaw":0.0}
            ],
            "center":[{"x":0.0, "y":135.0, "z":1485.0, "yaw":0.0}],
            "task1":[{"x":32.0, "y":58.0, "z":1485.0, "yaw":0.0}],
            "trader":[
                {"x":133.0, "y":61.0, "z":1504.0, "yaw":-90.0}, 
                {"x":-54.0, "y":62.0, "z":1581.0, "yaw":-90.0},
                {"x":-28.0, "y":62.0, "z":1406.0, "yaw":-90.0}
            ],
            "escapeArea":[
                {"x":-169, "y":58, "z":1488, "yaw":0.0},
                {"x":-149, "y":84, "z":1513, "yaw":0.0}
            ]
        }
    ]
    """
    // east, east, east
    // プレイヤー、チェストのスポーン可能座標を取得する
    fun getSpawnLocations() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val fileName = "mapsettings.json"
            val file = File(fileName)
            if (file.exists()) {
                val jsonArray = JSONParser().parse(FileReader(fileName)) as JSONArray
                for (i in 0 until jsonArray.size) {
                    val m = jsonArray[i] as JSONObject
                    val mN = m["mapName"] as String
                    val mC = m["mapContents"] as JSONArray
                    val mapContents = mutableListOf<ContentsLocations>()
                    for (j in 0 until mC.size) {
                        val c = mC[j] as JSONObject
                        val cN = c["contents"] as String
                        val cL = c["locations"] as JSONArray
                        val locations = mutableListOf<LocationXYZ>()
                        for (k in 0 until cL.size) {
                            val l = cL[k] as JSONObject
                            val x = l["x"] as Double
                            val y = l["y"] as Double
                            val z = l["z"] as Double
                            val yaw = l["yaw"] as Double
                            locations.add(LocationXYZ(x, y, z, yaw))
                        }
                        mapContents.add(ContentsLocations(cN, locations))
                    }
                    mapContentsLocations.add(MapContentsLocations(mN, mapContents))
                }
                plugin.logger.info("loading json") //test
            }
            else {
                // mapsetting.jsonがディレクトリ内にない場合
                val allMapSettingsJson = JSONArray()
                val stages = Json.decodeFromString<List<Stage>>(allMapSettings)
                for (stage in stages) {
                    // playerSpawnLocations, chestSpawnLocations, switchSpawnLocations
                    playerSpawnLocations.clear()
                    chestSpawnLocations.clear()
                    switchSpawnLocations.clear()
                    traderSpawnLocations.clear()
                    task1Location.clear()
                    mapCenterLocations.clear()
                    escapeArea.clear()

                    for (x in stage.mapRange[0].x.toInt()..stage.mapRange[1].x.toInt()) {
                        for (y in stage.mapRange[0].y.toInt()..stage.mapRange[1].y.toInt()) {
                            for (z in stage.mapRange[0].z.toInt()..stage.mapRange[1].z.toInt()) {
                                val block = Bukkit.getWorld("world")?.getBlockAt(x, y, z)
                                if (block?.type == Material.LODESTONE) {
                                    playerSpawnLocations.add(LocationXYZ(x.toDouble(), y.toDouble(), z.toDouble(), 0.0))
                                } else if (block?.type == Material.BEDROCK) {
                                    chestSpawnLocations.add(LocationXYZ(x.toDouble(), y.toDouble(), z.toDouble(), 0.0))
                                } else if (block?.type == Material.NETHERITE_BLOCK) {
                                    switchSpawnLocations.add(LocationXYZ(x.toDouble(), y.toDouble(), z.toDouble(), 0.0))
                                }
                            }
                        }
                    }
                    // traderSpawnLocations
                    for (loc in stage.trader) {
                        val traderSpawnLocation = LocationXYZ(loc.x, loc.y, loc.z, loc.yaw)
                        traderSpawnLocations.add(traderSpawnLocation)
                    }
                    // task1Location
                    for (loc in stage.task1) {
                        val task1 = LocationXYZ(loc.x, loc.y, loc.z, loc.yaw)
                        task1Location.add(task1)
                    }
                    // mapCenterLocations
                    for (loc in stage.center) {
                        val mapCenterLocation = LocationXYZ(loc.x, loc.y, loc.z, loc.yaw)
                        mapCenterLocations.add(mapCenterLocation)
                    }
                    // escapeArea
                    for (loc in stage.escapeArea) {
                        val escapeLocation = LocationXYZ(loc.x, loc.y, loc.z, loc.yaw)
                        escapeArea.add(escapeLocation)
                    }

                    val contentsLocations = listOf(
                        ContentsLocations("playerSpawnLocations", playerSpawnLocations),
                        ContentsLocations("chestSpawnLocations", chestSpawnLocations),
                        ContentsLocations("switchSpawnLocations", switchSpawnLocations),
                        ContentsLocations("traderSpawnLocations", traderSpawnLocations),
                        ContentsLocations("task1Location", task1Location),
                        ContentsLocations("mapCenterLocations", mapCenterLocations),
                        ContentsLocations("escapeArea", escapeArea),
                    )
                    val mapContents = MapContentsLocations(stage.mapName, contentsLocations.toMutableList())
                    mapContentsLocations.add(mapContents)

                    // ステージの座標情報をallMapSettingsJsonに追加
                    val jsonObject = JSONObject()
                    jsonObject["mapName"] = stage.mapName

                    val contentsLocationsArray = JSONArray()
                    for (cL in contentsLocations) {
                        // ContentsLocations
                        val mapContentsJson = JSONObject()
                        mapContentsJson["contents"] = cL.contents

                        val locationsArray = JSONArray()
                        for (sL in cL.locations) {
                            // playerSpawnLocations, chestSpawnLocations, switchSpawnLocationsの各座標
                            val locationObject = JSONObject()
                            locationObject["x"] = sL.x
                            locationObject["y"] = sL.y
                            locationObject["z"] = sL.z
                            locationObject["yaw"] = sL.yaw
                            locationsArray.add(locationObject)
                        }
                        mapContentsJson["locations"] = locationsArray

                        contentsLocationsArray.add(mapContentsJson)
                    }
                    jsonObject["mapContents"] = contentsLocationsArray

                    allMapSettingsJson.add(jsonObject)

                    plugin.logger.info("MAP情報を追加しました：${stage.mapName}")
                }
                // 全ステージ情報をJSONファイルに出力
                val newFile = FileWriter(fileName)
                newFile.write(allMapSettingsJson.toJSONString())
                newFile.flush()
                newFile.close()
            }
            plugin.logger.info("マップ情報をロードしました")
        })
    }
}