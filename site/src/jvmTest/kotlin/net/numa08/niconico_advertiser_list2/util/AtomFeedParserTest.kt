package net.numa08.niconico_advertiser_list2.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AtomFeedParserのテスト
 */
class AtomFeedParserTest {
    @Test
    fun testParseUserVideos_normalFeed() {
        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
                <updated>2025-11-10T21:02:05+09:00</updated>
                <entry>
                    <title>テスト動画1</title>
                    <link rel="alternate" href="https://www.nicovideo.jp/watch/sm43234567"/>
                    <published>2025-11-10T12:34:56+09:00</published>
                    <media:thumbnail url="https://example.com/thumbnail1.jpg"/>
                </entry>
                <entry>
                    <title>テスト動画2</title>
                    <link rel="alternate" href="https://www.nicovideo.jp/watch/sm43234568"/>
                    <published>2025-11-09T10:00:00+09:00</published>
                    <media:thumbnail url="https://example.com/thumbnail2.jpg"/>
                </entry>
            </feed>
            """.trimIndent()

        val videos = AtomFeedParser.parseUserVideos(atomXml)

        assertEquals(2, videos.size)

        // 1つ目の動画
        assertEquals("sm43234567", videos[0].videoId)
        assertEquals("テスト動画1", videos[0].title)
        assertEquals("https://example.com/thumbnail1.jpg", videos[0].thumbnail)
        assertEquals("2025-11-10T12:34:56+09:00", videos[0].published)
        assertEquals("https://www.nicovideo.jp/watch/sm43234567", videos[0].link)

        // 2つ目の動画
        assertEquals("sm43234568", videos[1].videoId)
        assertEquals("テスト動画2", videos[1].title)
    }

    @Test
    fun testParseUserVideos_emptyFeed() {
        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
                <updated>2025-11-10T21:02:05+09:00</updated>
            </feed>
            """.trimIndent()

        val videos = AtomFeedParser.parseUserVideos(atomXml)

        assertEquals(0, videos.size)
    }

    @Test
    fun testParseUserVideos_withoutMediaNamespace() {
        // media:thumbnail がない場合
        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
                <updated>2025-11-10T21:02:05+09:00</updated>
                <entry>
                    <title>テスト動画1</title>
                    <link rel="alternate" href="https://www.nicovideo.jp/watch/sm43234567"/>
                    <published>2025-11-10T12:34:56+09:00</published>
                </entry>
            </feed>
            """.trimIndent()

        val videos = AtomFeedParser.parseUserVideos(atomXml)

        assertEquals(1, videos.size)
        assertEquals("sm43234567", videos[0].videoId)
        assertEquals("", videos[0].thumbnail) // サムネイルがない場合は空文字
    }

    @Test
    fun testParseUserVideos_invalidEntry() {
        // リンクがないエントリは無視される
        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
                <updated>2025-11-10T21:02:05+09:00</updated>
                <entry>
                    <title>有効な動画</title>
                    <link rel="alternate" href="https://www.nicovideo.jp/watch/sm43234567"/>
                    <published>2025-11-10T12:34:56+09:00</published>
                    <media:thumbnail url="https://example.com/thumbnail1.jpg"/>
                </entry>
                <entry>
                    <title>無効な動画（リンクなし）</title>
                    <published>2025-11-09T10:00:00+09:00</published>
                </entry>
            </feed>
            """.trimIndent()

        val videos = AtomFeedParser.parseUserVideos(atomXml)

        // 無効なエントリは無視される
        assertEquals(1, videos.size)
        assertEquals("sm43234567", videos[0].videoId)
    }

    @Test
    fun testExtractFeedUpdated_normal() {
        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
                <updated>2025-11-10T21:02:05+09:00</updated>
            </feed>
            """.trimIndent()

        val updated = AtomFeedParser.extractFeedUpdated(atomXml)

        assertNotNull(updated)
        assertEquals("2025-11-10T21:02:05+09:00", updated)
    }

    @Test
    fun testExtractFeedUpdated_missing() {
        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
            </feed>
            """.trimIndent()

        val updated = AtomFeedParser.extractFeedUpdated(atomXml)

        // updatedがない場合はnull
        assertNull(updated)
    }

    @Test
    fun testExtractFeedUpdated_invalidXml() {
        val invalidXml = "this is not xml"

        val updated = AtomFeedParser.extractFeedUpdated(invalidXml)

        // 不正なXMLの場合はnull
        assertNull(updated)
    }

    @Test
    fun testParseUserVideos_multipleVideos() {
        // 42件の動画を含むfeed
        val entries =
            (1..42).joinToString("\n") { i ->
                """
                <entry>
                    <title>テスト動画$i</title>
                    <link rel="alternate" href="https://www.nicovideo.jp/watch/sm$i"/>
                    <published>2025-11-10T12:34:56+09:00</published>
                    <media:thumbnail url="https://example.com/thumbnail$i.jpg"/>
                </entry>
                """.trimIndent()
            }

        val atomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
                <title>テストユーザーさんの投稿動画‐ニコニコ動画</title>
                <updated>2025-11-10T21:02:05+09:00</updated>
                $entries
            </feed>
            """.trimIndent()

        val videos = AtomFeedParser.parseUserVideos(atomXml)

        assertEquals(42, videos.size)
        assertEquals("sm1", videos[0].videoId)
        assertEquals("sm42", videos[41].videoId)
    }
}
