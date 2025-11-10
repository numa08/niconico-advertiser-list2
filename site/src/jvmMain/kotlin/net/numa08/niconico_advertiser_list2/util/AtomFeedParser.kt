package net.numa08.niconico_advertiser_list2.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.parser.Parser
import net.numa08.niconico_advertiser_list2.models.UserVideo

/**
 * ニコニコ動画のAtom feedをパースするユーティリティ
 */
object AtomFeedParser {
    /**
     * Atom feedをパースしてUserVideoのリストを返す
     *
     * @param atomXml Atom feed XML文字列
     * @return パースされた動画リスト
     */
    fun parseUserVideos(atomXml: String): List<UserVideo> {
        val doc = Ksoup.parse(atomXml, Parser.xmlParser())
        val entries = doc.select("entry")

        return entries.mapNotNull { entry ->
            try {
                // リンク要素から動画URLを取得
                val link = entry.select("link[rel=alternate]").attr("href")
                if (link.isEmpty()) {
                    return@mapNotNull null
                }

                val videoId = extractVideoId(link)
                val title = entry.select("title").text()

                // media:thumbnail (名前空間付き要素)
                // ksoupでは | を使って名前空間を指定
                val thumbnail =
                    entry.select("thumbnail").attr("url")
                        .ifEmpty { entry.select("media|thumbnail").attr("url") }

                val published = entry.select("published").text()

                UserVideo(
                    videoId = videoId,
                    title = title,
                    thumbnail = thumbnail,
                    published = published,
                    link = link,
                )
            } catch (e: Exception) {
                // パースエラーは無視してnullを返す
                null
            }
        }
    }

    /**
     * feedの更新日時を取得
     *
     * @param atomXml Atom feed XML文字列
     * @return 更新日時（ISO8601形式）、取得できない場合はnull
     */
    fun extractFeedUpdated(atomXml: String): String? =
        try {
            val doc = Ksoup.parse(atomXml, Parser.xmlParser())
            val updated = doc.select("feed > updated").text()
            updated.ifEmpty { null }
        } catch (e: Exception) {
            null
        }

    /**
     * URLから動画IDを抽出
     *
     * @param url 動画URL（例: https://www.nicovideo.jp/watch/sm43234567）
     * @return 動画ID（例: sm43234567）
     */
    private fun extractVideoId(url: String): String = url.substringAfterLast("/")
}
