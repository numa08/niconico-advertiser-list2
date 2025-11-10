package net.numa08.niconico_advertiser_list2.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * UserIdExtractorのテスト
 */
class UserIdExtractorTest {
    @Test
    fun testExtractUserId_directId() {
        // 直接ID
        assertEquals("753685", UserIdExtractor.extractUserId("753685"))
        assertEquals("123456", UserIdExtractor.extractUserId("123456"))
    }

    @Test
    fun testExtractUserId_userPageUrl() {
        // ユーザーページURL
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/753685"),
        )
        assertEquals(
            "123456",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/123456"),
        )
    }

    @Test
    fun testExtractUserId_userVideoUrl() {
        // ユーザー動画ページURL
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/753685/video"),
        )
        assertEquals(
            "123456",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/123456/video"),
        )
    }

    @Test
    fun testExtractUserId_rssUrl() {
        // RSS URL
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/753685/video?rss=atom"),
        )
        assertEquals(
            "123456",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/123456/video?rss=atom&page=1"),
        )
    }

    @Test
    fun testExtractUserId_withFragment() {
        // フラグメント付きURL
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/753685#tab"),
        )
    }

    @Test
    fun testExtractUserId_httpScheme() {
        // HTTPスキーム
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("http://www.nicovideo.jp/user/753685"),
        )
    }

    @Test
    fun testExtractUserId_subdomain() {
        // サブドメイン
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("https://sp.nicovideo.jp/user/753685"),
        )
    }

    @Test
    fun testExtractUserId_withWhitespace() {
        // 前後の空白
        assertEquals("753685", UserIdExtractor.extractUserId("  753685  "))
        assertEquals(
            "753685",
            UserIdExtractor.extractUserId("  https://www.nicovideo.jp/user/753685  "),
        )
    }

    @Test
    fun testExtractUserId_invalid() {
        // 無効な入力
        assertNull(UserIdExtractor.extractUserId("abc"))
        assertNull(UserIdExtractor.extractUserId("sm12345678")) // 動画IDではない
        assertNull(UserIdExtractor.extractUserId(""))
        assertNull(UserIdExtractor.extractUserId("   "))
    }

    @Test
    fun testExtractUserId_invalidUrl() {
        // 無効なURL
        assertNull(UserIdExtractor.extractUserId("https://example.com/user/753685"))
        assertNull(UserIdExtractor.extractUserId("https://www.nicovideo.jp/watch/sm12345678"))
        assertNull(UserIdExtractor.extractUserId("https://www.nicovideo.jp/"))
        assertNull(UserIdExtractor.extractUserId("not a url"))
    }

    @Test
    fun testExtractUserId_alphanumericUserId() {
        // 英数字混合は無効（ユーザーIDは数字のみ）
        assertNull(UserIdExtractor.extractUserId("abc123"))
        assertNull(UserIdExtractor.extractUserId("https://www.nicovideo.jp/user/abc123"))
    }
}
