package net.numa08.niconico_advertiser_list2.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * VideoIdExtractorのユニットテスト
 */
class VideoIdExtractorTest {
    // 動画ID直接入力のテスト
    @Test
    fun extractVideoId_withDirectVideoId_returnsVideoId() {
        // 正常系: 動画IDを直接入力
        val input = "sm12345678"
        val result = VideoIdExtractor.extractVideoId(input)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withDirectVideoIdAndWhitespace_returnsVideoId() {
        // 正常系: 動画IDを前後に空白付きで入力
        val input = "  sm12345678  "
        val result = VideoIdExtractor.extractVideoId(input)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withDirectVideoIdSingleDigit_returnsVideoId() {
        // 正常系: 1桁の数字の動画ID
        val input = "sm1"
        val result = VideoIdExtractor.extractVideoId(input)
        assertEquals("sm1", result)
    }

    @Test
    fun extractVideoId_withDirectSmOnly_returnsNull() {
        // 異常系: smのみ（動画ID直接入力）
        val input = "sm"
        val result = VideoIdExtractor.extractVideoId(input)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withDirectNonNumericAfterSm_returnsNull() {
        // 異常系: sm以降が数字でない（動画ID直接入力）
        val input = "smabc123"
        val result = VideoIdExtractor.extractVideoId(input)
        assertNull(result)
    }

    // URL形式のテスト
    @Test
    fun extractVideoId_withValidHttpsWwwUrl_returnsVideoId() {
        // 正常系: https://www.nicovideo.jp/watch/sm12345678
        val url = "https://www.nicovideo.jp/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withValidHttpsUrl_returnsVideoId() {
        // 正常系: https://nicovideo.jp/watch/sm12345678
        val url = "https://nicovideo.jp/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withValidHttpUrl_returnsVideoId() {
        // 正常系: http://www.nicovideo.jp/watch/sm12345678
        val url = "http://www.nicovideo.jp/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withSingleDigit_returnsVideoId() {
        // 正常系: 1桁の数字
        val url = "https://www.nicovideo.jp/watch/sm1"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm1", result)
    }

    @Test
    fun extractVideoId_withSubdomain_returnsVideoId() {
        // 正常系: サブドメイン付き
        val url = "https://sp.nicovideo.jp/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withQueryParameter_returnsVideoId() {
        // 正常系: クエリパラメータ付き
        val url = "https://www.nicovideo.jp/watch/sm12345678?ref=top"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withFragment_returnsVideoId() {
        // 正常系: フラグメント付き
        val url = "https://www.nicovideo.jp/watch/sm12345678#comments"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withQueryAndFragment_returnsVideoId() {
        // 正常系: クエリパラメータとフラグメント両方
        val url = "https://www.nicovideo.jp/watch/sm12345678?ref=top#comments"
        val result = VideoIdExtractor.extractVideoId(url)
        assertEquals("sm12345678", result)
    }

    @Test
    fun extractVideoId_withEmptyString_returnsNull() {
        // 異常系: 空文字
        val url = ""
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withBlankString_returnsNull() {
        // 異常系: 空白文字
        val url = "   "
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withoutScheme_returnsNull() {
        // 異常系: スキームなし
        val url = "www.nicovideo.jp/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withDifferentDomain_returnsNull() {
        // 異常系: 違うドメイン
        val url = "https://www.youtube.com/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withoutWatchPath_returnsNull() {
        // 異常系: /watch/パスがない
        val url = "https://www.nicovideo.jp/video/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withoutSmPrefix_returnsNull() {
        // 異常系: smで始まらない
        val url = "https://www.nicovideo.jp/watch/12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withSmOnly_returnsNull() {
        // 異常系: smだけ
        val url = "https://www.nicovideo.jp/watch/sm"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withNonNumericAfterSm_returnsNull() {
        // 異常系: sm以降が数字でない
        val url = "https://www.nicovideo.jp/watch/smabc123"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withMixedCaseSmPrefix_returnsNull() {
        // 異常系: SMが大文字（小文字のsmのみ許可）
        val url = "https://www.nicovideo.jp/watch/SM12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }

    @Test
    fun extractVideoId_withDomainSuffix_returnsNull() {
        // 異常系: nicovideo.jpで終わるが、正しいドメインではない
        val url = "https://fakenicovideo.jp/watch/sm12345678"
        val result = VideoIdExtractor.extractVideoId(url)
        assertNull(result)
    }
}
