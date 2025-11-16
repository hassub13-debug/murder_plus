package me.haskk006.murder_plus

import org.bukkit.ChatColor
import org.bukkit.event.Listener
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class QuestBook(private val gameDirection: GameDirection) : Listener {

    fun giveQuestBook(player : Player) {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val bookMeta = book.itemMeta as BookMeta
        bookMeta.title = "ガイドブック"
        bookMeta.author = "Author"
        val pages = listOf(
            "${ChatColor.DARK_AQUA}ゲームのルール\n" +
                    "${ChatColor.DARK_RED}マーダー：\n" +
                    "${ChatColor.BLACK}勝利条件1：サバイバーの全滅\n" +
                    "${ChatColor.BLACK}\n" +
                    "${ChatColor.DARK_GREEN}サバイバー：\n" +
                    "${ChatColor.BLACK}勝利条件1：脱出タスクをすべてクリアして、脱出地点に到達\n" +
                    "${ChatColor.BLACK}勝利条件2：マーダーの全滅\n" +
                    "${ChatColor.BLACK}\n" +
                    "${ChatColor.GOLD}その他：\n" +
                    "${ChatColor.BLACK}チェストから手に入るアイテムを有効活用しよう\n",

            "${ChatColor.DARK_AQUA}他になんか書くところ\n" +
                    "${ChatColor.BLACK}( ᐛ )\n"
        )
        bookMeta.pages = pages // ページを本に追加

        book.itemMeta = bookMeta

        player.inventory.setItem(7, book)  // ホットバーの左から8番目に本を配置
    }
}
