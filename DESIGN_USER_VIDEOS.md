# ユーザー動画一覧機能 - 詳細設計書

## 目次
1. [概要](#概要)
2. [データモデル](#データモデル)
3. [バックエンド実装](#バックエンド実装)
4. [フロントエンド実装](#フロントエンド実装)
5. [実装計画](#実装計画)
6. [テストケース](#テストケース)

---

## 概要

### 目的
動画投稿者が自分のユーザーIDを設定することで、トップページに自分の投稿動画一覧を表示し、動画を選択して広告者リストを簡単に確認できる機能を提供する。

### 主な機能
- ユーザーIDまたはユーザーURLの入力と保存
- ニコニコ動画のAtom feedを使った投稿動画一覧の取得
- ページネーション対応（page=1, 2, 3...）
- ページごとのCaffeineキャッシュ（30分）
- 動画選択から広告者リストページへの遷移

### 技術仕様
- **Atom Feed**: `https://www.nicovideo.jp/user/{userId}/video?rss=atom&page={page}`
- **ページネーション**: page=1から開始、存在しないページは空のfeedを返す
- **キャッシュ**: Caffeineのローダー機能を使用
- **パース**: ksoupを使用してXMLパース

---

## データモデル

### 1. UserVideo (commonMain/models/UserVideo.kt)

```kotlin
package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ユーザーの投稿動画情報
 */
@Serializable
data class UserVideo(
    val videoId: String,        // 動画ID (例: sm43234567)
    val title: String,          // 動画タイトル
    val thumbnail: String,      // サムネイルURL
    val published: String,      // 投稿日時 (ISO8601形式)
    val link: String            // 動画URL
)
```

### 2. UserVideosResponse (commonMain/models/UserVideosResponse.kt)

```kotlin
package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ユーザー動画一覧のレスポンス
 */
@Serializable
data class UserVideosResponse(
    val userId: String,         // ユーザーID
    val page: Int,              // 現在のページ番号
    val videos: List<UserVideo>, // 動画リスト
    val videosCount: Int,       // このページの動画件数
    val hasNext: Boolean,       // 次のページがあるか（推定）
    val feedUpdated: String?,   // feedの更新日時
    val cachedAt: String?,      // キャッシュ日時
    val fromCache: Boolean = false // キャッシュから取得したか
)
```

---

## バックエンド実装

### 1. UserIdExtractor (commonMain/util/UserIdExtractor.kt)

ユーザーIDまたはユーザーURLからユーザーIDを抽出するユーティリティ。
VideoIdExtractorと同様の構造。

```kotlin
package net.numa08.niconico_advertiser_list2.util

/**
 * ニコニコ動画のユーザーURLからユーザーIDを抽出するユーティリティ
 */
object UserIdExtractor {
    /**
     * URLまたはユーザーIDからユーザーIDを抽出する
     *
     * @param input ニコニコ動画のユーザーURLまたはユーザーID（例: 92164945）
     * @return 抽出されたユーザーID。抽出できない場合はnull
     *
     * サポートするパターン:
     * - 直接ID: "92164945"
     * - ユーザーページURL: "https://www.nicovideo.jp/user/92164945"
     * - ユーザー動画URL: "https://www.nicovideo.jp/user/92164945/video"
     * - RSS URL: "https://www.nicovideo.jp/user/92164945/video?rss=atom"
     */
    fun extractUserId(input: String): String? {
        // 空文字チェック
        if (input.isBlank()) return null

        val trimmedInput = input.trim()

        // まず、直接ユーザーIDとして有効かチェック（数字のみ）
        if (isValidUserId(trimmedInput)) {
            return trimmedInput
        }

        // URLとしてパース
        val parsedUrl = try {
            parseUrl(trimmedInput)
        } catch (e: Exception) {
            return null
        }

        // ドメインチェック（nicovideo.jpまたはそのサブドメイン）
        if (!isNiconicoVideoDomain(parsedUrl.host)) {
            return null
        }

        // パスからユーザーIDを抽出
        return extractUserIdFromPath(parsedUrl.path)
    }

    /**
     * ドメインがnicovideo.jpまたはそのサブドメインかチェック
     */
    private fun isNiconicoVideoDomain(host: String): Boolean {
        val lowerHost = host.lowercase()
        return lowerHost == "nicovideo.jp" || lowerHost.endsWith(".nicovideo.jp")
    }

    /**
     * パスからユーザーIDを抽出
     * パターン: /user/{userId} または /user/{userId}/video
     */
    private fun extractUserIdFromPath(path: String): String? {
        // /user/{userId} のパターンをチェック
        val userPrefix = "/user/"
        if (!path.startsWith(userPrefix)) {
            return null
        }

        // /user/の後の部分を取得
        val userId = path
            .substring(userPrefix.length)
            .split("/")
            .first()
            .split("?")
            .first()
            .split("#")
            .first()

        // 数字のみのパターンをチェック
        if (!isValidUserId(userId)) {
            return null
        }

        return userId
    }

    /**
     * ユーザーIDが有効な形式（数字のみ）かチェック
     */
    private fun isValidUserId(userId: String): Boolean {
        if (userId.isEmpty()) {
            return false
        }
        return userId.all { it.isDigit() }
    }

    /**
     * 簡易的なURLパーサー
     */
    private fun parseUrl(url: String): ParsedUrl {
        // スキームの抽出
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) {
            throw IllegalArgumentException("Invalid URL: missing scheme")
        }

        val scheme = url.substring(0, schemeEnd)
        val remaining = url.substring(schemeEnd + 3)

        // ホストとパスの分離
        val pathStart = remaining.indexOf('/')
        val host: String
        val path: String

        if (pathStart == -1) {
            host = remaining
            path = "/"
        } else {
            host = remaining.substring(0, pathStart)
            path = remaining.substring(pathStart)
        }

        return ParsedUrl(scheme, host, path)
    }

    /**
     * パース済みURL
     */
    private data class ParsedUrl(
        val scheme: String,
        val host: String,
        val path: String,
    )
}
```

### 2. AtomFeedParser (jvmMain/util/AtomFeedParser.kt)

Atom feedをパースして動画情報を抽出。

```kotlin
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
                val thumbnail = entry.select("thumbnail").attr("url")
                    .ifEmpty { entry.select("media|thumbnail").attr("url") }

                val published = entry.select("published").text()

                UserVideo(
                    videoId = videoId,
                    title = title,
                    thumbnail = thumbnail,
                    published = published,
                    link = link
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
    fun extractFeedUpdated(atomXml: String): String? {
        return try {
            val doc = Ksoup.parse(atomXml, Parser.xmlParser())
            doc.select("feed > updated").text().ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * URLから動画IDを抽出
     *
     * @param url 動画URL（例: https://www.nicovideo.jp/watch/sm43234567）
     * @return 動画ID（例: sm43234567）
     */
    private fun extractVideoId(url: String): String {
        return url.substringAfterLast("/")
    }
}
```

### 3. VideoCacheService拡張 (jvmMain/cache/VideoCacheService.kt)

既存のVideoCacheServiceにユーザー動画リストのキャッシュを追加。
Caffeineのローダー機能を使用。

```kotlin
// VideoCacheService.kt に以下を追加

/**
 * ユーザー動画リストとキャッシュメタデータ
 */
data class CachedUserVideos(
    val userId: String,
    val page: Int,
    val videos: List<UserVideo>,
    val videosCount: Int,
    val feedUpdated: String?,
    val cachedAt: Instant,
)

/**
 * ユーザー動画リストのキャッシュキー
 */
data class UserVideosCacheKey(
    val userId: String,
    val page: Int,
)

// VideoCacheServiceオブジェクト内に追加
object VideoCacheService {
    // ... 既存のコード ...

    private const val USER_VIDEOS_CACHE_TTL_MINUTES = 30L

    /**
     * ユーザー動画リストのLoadingCache
     * キャッシュミス時は自動的にAtom feedから取得
     */
    val userVideosCache: LoadingCache<UserVideosCacheKey, CachedUserVideos> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(USER_VIDEOS_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MAX_CACHE_SIZE)
            .recordStats()
            .build { key ->
                // キャッシュミス時に自動実行されるローダー
                runBlocking {
                    RequestSemaphore.withLimit {
                        val atomXml = dataSource.fetchUserVideosFeed(key.userId, key.page)
                        val videos = AtomFeedParser.parseUserVideos(atomXml)
                        val feedUpdated = AtomFeedParser.extractFeedUpdated(atomXml)

                        CachedUserVideos(
                            userId = key.userId,
                            page = key.page,
                            videos = videos,
                            videosCount = videos.size,
                            feedUpdated = feedUpdated,
                            cachedAt = Instant.now()
                        )
                    }
                }
            }

    /**
     * ユーザー動画リストを取得
     * キャッシュにあればキャッシュから、なければ自動的にロードして返す
     *
     * @param userId ユーザーID
     * @param page ページ番号
     * @return Result<CachedUserVideos> 成功時はキャッシュされた動画リスト、失敗時はエラー
     */
    fun getUserVideos(userId: String, page: Int): Result<CachedUserVideos> =
        runCatching {
            userVideosCache.get(UserVideosCacheKey(userId, page))
        }

    /**
     * ユーザー動画リストを強制更新
     * キャッシュを削除して再取得
     *
     * @param userId ユーザーID
     * @param page ページ番号
     * @return Result<CachedUserVideos> 成功時はキャッシュされた動画リスト、失敗時はエラー
     */
    fun refreshUserVideos(userId: String, page: Int): Result<CachedUserVideos> =
        runCatching {
            val key = UserVideosCacheKey(userId, page)
            userVideosCache.invalidate(key)
            userVideosCache.get(key)
        }

    /**
     * 特定ユーザーの全ページのキャッシュを削除
     */
    fun invalidateUserVideos(userId: String) {
        // Caffeineには特定のキーパターンで削除する機能がないため
        // 全キャッシュをスキャンして該当するものを削除
        userVideosCache.asMap().keys
            .filter { it.userId == userId }
            .forEach { userVideosCache.invalidate(it) }
    }
}
```

### 4. NiconicoDataSource拡張 (jvmMain/datasource/NicoadDataSource.kt)

Atom feedを取得するメソッドを追加。

```kotlin
// NicoadDataSource.kt に以下のメソッドを追加

/**
 * ユーザーの投稿動画一覧をAtom feedで取得
 *
 * @param userId ユーザーID
 * @param page ページ番号（デフォルト: 1）
 * @return Atom feed XML文字列
 * @throws VideoNotFoundException ユーザーが存在しない場合
 */
suspend fun fetchUserVideosFeed(userId: String, page: Int = 1): String {
    val url = "https://www.nicovideo.jp/user/$userId/video?rss=atom&page=$page"

    return try {
        val response = httpClient.get(url)

        when (response.status.value) {
            404 -> throw VideoNotFoundException("User not found: $userId")
            in 200..299 -> response.bodyAsText()
            else -> throw Exception("Failed to fetch user videos: ${response.status}")
        }
    } catch (e: VideoNotFoundException) {
        throw e
    } catch (e: Exception) {
        throw Exception("Failed to fetch user videos feed: ${e.message}", e)
    }
}
```

### 5. UserVideos API (jvmMain/api/UserVideos.kt)

新しいAPIエンドポイント。

```kotlin
package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBodyText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.cache.VideoCacheService
import net.numa08.niconico_advertiser_list2.datasource.VideoNotFoundException
import net.numa08.niconico_advertiser_list2.models.UserVideosResponse

/**
 * ユーザーの投稿動画一覧を取得するAPIエンドポイント
 *
 * GET /api/user/videos?userId={userId}&page={page}&refresh={true|false}
 *
 * @param userId ユーザーID（必須）
 * @param page ページ番号（オプション、デフォルト: 1）
 * @param refresh キャッシュを無視して再取得（オプション、デフォルト: false）
 */
@Api("user/videos")
suspend fun userVideos(ctx: ApiContext) {
    try {
        // パラメータ取得
        val userId = ctx.req.params["userId"]
            ?: run {
                ctx.res.status = 400
                ctx.res.setBodyText("Missing required parameter: userId")
                return
            }

        val page = ctx.req.params["page"]?.toIntOrNull() ?: 1
        val refresh = ctx.req.params["refresh"]?.toBoolean() ?: false

        // パラメータバリデーション
        if (page < 1) {
            ctx.res.status = 400
            ctx.res.setBodyText("Invalid parameter: page must be >= 1")
            return
        }

        // ユーザーIDのバリデーション（数字のみ）
        if (!userId.all { it.isDigit() }) {
            ctx.res.status = 400
            ctx.res.setBodyText("Invalid parameter: userId must be numeric")
            return
        }

        // データ取得
        val result = if (refresh) {
            VideoCacheService.refreshUserVideos(userId, page)
        } else {
            VideoCacheService.getUserVideos(userId, page)
        }

        result.fold(
            onSuccess = { cached ->
                // hasNextの判定（シンプルな方法）
                val hasNext = cached.videosCount > 0

                val response = UserVideosResponse(
                    userId = cached.userId,
                    page = cached.page,
                    videos = cached.videos,
                    videosCount = cached.videosCount,
                    hasNext = hasNext,
                    feedUpdated = cached.feedUpdated,
                    cachedAt = cached.cachedAt.toString(),
                    fromCache = !refresh
                )

                ctx.res.status = 200
                ctx.res.setBodyText(Json.encodeToString(response))
            },
            onFailure = { error ->
                when (error) {
                    is VideoNotFoundException -> {
                        ctx.res.status = 404
                        ctx.res.setBodyText("User not found: $userId")
                    }
                    else -> {
                        ctx.res.status = 500
                        ctx.res.setBodyText("Internal server error: ${error.message}")
                    }
                }
            }
        )
    } catch (e: Exception) {
        ctx.res.status = 500
        ctx.res.setBodyText("Internal server error: ${e.message}")
    }
}
```

---

## フロントエンド実装

### 1. UserPreferencesService (jsMain/util/UserPreferencesService.kt)

LocalStorageを使ったユーザー設定の永続化。

```kotlin
package net.numa08.niconico_advertiser_list2.util

import kotlinx.browser.window

/**
 * ユーザー設定をLocalStorageで管理するサービス
 */
object UserPreferencesService {
    private const val USER_ID_KEY = "niconico_user_id"

    /**
     * 保存されているユーザーIDを取得
     *
     * @return ユーザーID、保存されていない場合はnull
     */
    fun getUserId(): String? {
        return window.localStorage.getItem(USER_ID_KEY)
    }

    /**
     * ユーザーIDを保存
     *
     * @param userId 保存するユーザーID
     */
    fun setUserId(userId: String) {
        window.localStorage.setItem(USER_ID_KEY, userId)
    }

    /**
     * 保存されているユーザーIDを削除
     */
    fun clearUserId() {
        window.localStorage.removeItem(USER_ID_KEY)
    }

    /**
     * ユーザーIDが設定されているか確認
     *
     * @return 設定されている場合true
     */
    fun hasUserId(): Boolean {
        return getUserId() != null
    }
}
```

### 2. UserIdSettingDialog (jsMain/components/UserIdSettingDialog.kt)

ユーザーID設定用のモーダルダイアログ。

```kotlin
package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import net.numa08.niconico_advertiser_list2.util.UserIdExtractor
import net.numa08.niconico_advertiser_list2.util.UserPreferencesService
import org.jetbrains.compose.web.css.*

/**
 * ユーザーID設定ダイアログ
 *
 * @param isOpen ダイアログが開いているか
 * @param onClose ダイアログを閉じる際のコールバック
 * @param onSave ユーザーIDが保存された際のコールバック
 */
@Composable
fun UserIdSettingDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!isOpen) return

    val theme = Theme.current
    var userIdInput by remember { mutableStateOf(UserPreferencesService.getUserId() ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // モーダルオーバーレイ
    Box(
        modifier = Modifier
            .position(Position.Fixed)
            .top(0.px)
            .left(0.px)
            .width(100.percent)
            .height(100.percent)
            .backgroundColor(rgba(0, 0, 0, 0.5))
            .zIndex(1000)
            .onClick { onClose() },
        contentAlignment = Alignment.Center
    ) {
        // ダイアログコンテンツ
        Box(
            modifier = Modifier
                .backgroundColor(theme.surface)
                .borderRadius(12.px)
                .padding(2.cssRem)
                .maxWidth(500.px)
                .width(90.percent)
                .onClick { it.stopPropagation() } // クリックイベントの伝播を停止
        ) {
            Column(modifier = Modifier.gap(1.5.cssRem)) {
                // タイトル
                SpanText(
                    "投稿者IDを設定",
                    modifier = Modifier
                        .fontSize(1.5.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(theme.onSurface)
                )

                // 説明文
                Column(modifier = Modifier.gap(0.5.cssRem)) {
                    SpanText(
                        "あなたのニコニコ動画のユーザーIDまたはユーザーページURLを入力してください",
                        modifier = Modifier
                            .fontSize(0.9.cssRem)
                            .color(theme.onSurfaceVariant)
                    )
                    SpanText(
                        "例: 92164945 または https://www.nicovideo.jp/user/92164945",
                        modifier = Modifier
                            .fontSize(0.85.cssRem)
                            .color(theme.onSurfaceVariant)
                            .fontStyle(FontStyle.Italic)
                    )
                }

                // 入力フィールド
                TextInput(
                    text = userIdInput,
                    onTextChange = {
                        userIdInput = it
                        errorMessage = null
                    },
                    placeholder = "ユーザーID または URL",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.75.cssRem)
                        .borderRadius(6.px)
                        .fontSize(1.cssRem)
                )

                // エラーメッセージ
                errorMessage?.let { error ->
                    SpanText(
                        error,
                        modifier = Modifier
                            .fontSize(0.9.cssRem)
                            .color(theme.error)
                    )
                }

                // ボタン
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .gap(1.cssRem),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onClose() },
                        modifier = Modifier
                            .flexGrow(1)
                            .padding(0.75.cssRem)
                            .backgroundColor(theme.surfaceVariant)
                            .color(theme.onSurfaceVariant)
                    ) {
                        SpanText("キャンセル")
                    }

                    Button(
                        onClick = {
                            val userId = UserIdExtractor.extractUserId(userIdInput)
                            if (userId != null) {
                                UserPreferencesService.setUserId(userId)
                                onSave(userId)
                                onClose()
                            } else {
                                errorMessage = "有効なユーザーIDまたはURLを入力してください"
                            }
                        },
                        modifier = Modifier
                            .flexGrow(1)
                            .padding(0.75.cssRem)
                            .backgroundColor(theme.primary)
                            .color(theme.onPrimary)
                            .fontWeight(FontWeight.Bold)
                    ) {
                        SpanText("保存")
                    }
                }
            }
        }
    }
}
```

### 3. VideoCard (jsMain/components/VideoCard.kt)

動画一覧の個別カード。

```kotlin
package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.ObjectFit
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.UpdateHistoryMode
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.models.UserVideo
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import kotlin.js.Date

/**
 * 動画カードコンポーネント
 *
 * @param video 動画情報
 */
@Composable
fun VideoCard(video: UserVideo) {
    val ctx = rememberPageContext()
    val theme = Theme.current

    Column(
        modifier = Modifier
            .borderRadius(8.px)
            .backgroundColor(theme.surfaceVariant)
            .padding(1.cssRem)
            .gap(0.75.cssRem)
            .cursor(Cursor.Pointer)
            .onClick {
                // 広告者ページへ遷移
                ctx.router.navigateTo(
                    "/advertisers/${video.videoId}",
                    updateHistoryMode = UpdateHistoryMode.PUSH
                )
            }
            .onMouseEnter {
                // ホバー時のスタイルはCSSで定義可能
            }
    ) {
        // サムネイル
        Image(
            src = video.thumbnail,
            alt = video.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.px)
                .borderRadius(4.px)
                .objectFit(ObjectFit.Cover)
        )

        // タイトル
        SpanText(
            video.title,
            modifier = Modifier
                .fontSize(1.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(theme.onSurface)
                .lineHeight(1.4)
                // 2行で切り捨て
                .styleModifier {
                    property("display", "-webkit-box")
                    property("-webkit-line-clamp", "2")
                    property("-webkit-box-orient", "vertical")
                    property("overflow", "hidden")
                }
        )

        // 動画ID
        SpanText(
            video.videoId,
            modifier = Modifier
                .fontSize(0.85.cssRem)
                .color(theme.onSurfaceVariant)
        )

        // 投稿日時
        SpanText(
            formatDate(video.published),
            modifier = Modifier
                .fontSize(0.8.cssRem)
                .color(theme.onSurfaceVariant)
        )
    }
}

/**
 * ISO8601形式の日時を読みやすい形式に変換
 */
private fun formatDate(isoString: String): String {
    return try {
        val date = Date(isoString)
        val year = date.getFullYear()
        val month = date.getMonth() + 1
        val day = date.getDate()
        "${year}年${month}月${day}日"
    } catch (e: Exception) {
        isoString
    }
}
```

### 4. Pagination (jsMain/components/Pagination.kt)

ページネーションコントロール。

```kotlin
package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.cssRem

/**
 * ページネーションコンポーネント
 *
 * @param currentPage 現在のページ番号
 * @param hasNext 次のページがあるか
 * @param onPrevious 前のページボタンのクリックハンドラ
 * @param onNext 次のページボタンのクリックハンドラ
 */
@Composable
fun Pagination(
    currentPage: Int,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val theme = Theme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .gap(1.cssRem),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 前へボタン
        Button(
            onClick = onPrevious,
            enabled = currentPage > 1,
            modifier = Modifier
                .padding(topBottom = 0.75.cssRem, leftRight = 1.5.cssRem)
                .backgroundColor(if (currentPage > 1) theme.primary else theme.surfaceVariant)
                .color(if (currentPage > 1) theme.onPrimary else theme.onSurfaceVariant)
        ) {
            SpanText("← 前へ")
        }

        // ページ番号表示
        SpanText(
            "ページ $currentPage",
            modifier = Modifier
                .flexGrow(1)
                .textAlign(com.varabyte.kobweb.compose.css.TextAlign.Center)
                .fontSize(1.cssRem)
                .fontWeight(FontWeight.Medium)
                .color(theme.onSurface)
        )

        // 次へボタン
        Button(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier
                .padding(topBottom = 0.75.cssRem, leftRight = 1.5.cssRem)
                .backgroundColor(if (hasNext) theme.primary else theme.surfaceVariant)
                .color(if (hasNext) theme.onPrimary else theme.onSurfaceVariant)
        ) {
            SpanText("次へ →")
        }
    }
}
```

### 5. Index.kt リファクタリング

トップページを大幅にリファクタリング。

```kotlin
package net.numa08.niconico_advertiser_list2.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.components.*
import net.numa08.niconico_advertiser_list2.models.UserVideosResponse
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import net.numa08.niconico_advertiser_list2.util.UserPreferencesService
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.w3c.fetch.RequestInit

/**
 * トップページ
 */
@Page("/")
@Composable
fun HomePage() {
    val theme = Theme.current
    val scope = rememberCoroutineScope()

    // 状態管理
    var userId by remember { mutableStateOf(UserPreferencesService.getUserId()) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var videosResponse by remember { mutableStateOf<UserVideosResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ユーザーIDが変更されたら動画を取得
    LaunchedEffect(userId, currentPage) {
        if (userId != null) {
            isLoading = true
            errorMessage = null

            scope.launch {
                try {
                    val response = window.fetch(
                        "/api/user/videos?userId=$userId&page=$currentPage",
                        RequestInit()
                    ).await()

                    if (response.ok) {
                        val json = response.text().await()
                        videosResponse = Json.decodeFromString<UserVideosResponse>(json)
                    } else {
                        errorMessage = when (response.status.toInt()) {
                            404 -> "ユーザーが見つかりませんでした"
                            else -> "動画の取得に失敗しました: ${response.statusText}"
                        }
                        videosResponse = null
                    }
                } catch (e: Exception) {
                    errorMessage = "エラーが発生しました: ${e.message}"
                    videosResponse = null
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.cssRem, bottom = 4.cssRem, leftRight = 2.cssRem)
            .gap(2.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // タイトル
        SpanText(
            text = "ニコニコ動画 広告主リスト取得",
            modifier = Modifier
                .fontSize(2.5.cssRem)
                .fontWeight(FontWeight.Bold)
                .textAlign(TextAlign.Center)
                .color(theme.onBackground)
        )

        // 説明文
        SpanText(
            text = "動画URLまたは動画IDを入力すると、広告主のリストを整形して表示します。感謝メッセージの作成などにご利用ください。",
            modifier = Modifier
                .fontSize(1.1.cssRem)
                .textAlign(TextAlign.Center)
                .maxWidth(600.px)
                .color(theme.onSurfaceVariant)
        )

        // ユーザーID設定状態による分岐
        if (userId == null) {
            // ユーザーID未設定時
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .maxWidth(600.px)
                    .gap(1.cssRem),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 動画検索フォーム
                VideoSearchForm(
                    onError = { error -> errorMessage = error }
                )

                // または
                SpanText(
                    "または",
                    modifier = Modifier
                        .fontSize(1.cssRem)
                        .color(theme.onSurfaceVariant)
                )

                // 投稿者ID設定ボタン
                Button(
                    onClick = { isDialogOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.cssRem)
                        .backgroundColor(theme.secondaryContainer)
                        .color(theme.onSecondaryContainer)
                ) {
                    SpanText("投稿者IDを設定して自分の動画から選択")
                }

                SpanText(
                    "投稿者IDを設定すると、あなたの動画一覧から選択できます",
                    modifier = Modifier
                        .fontSize(0.9.cssRem)
                        .textAlign(TextAlign.Center)
                        .color(theme.onSurfaceVariant)
                )
            }
        } else {
            // ユーザーID設定済み時
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .maxWidth(900.px)
                    .gap(1.5.cssRem)
            ) {
                // ユーザー情報バー
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.cssRem)
                        .backgroundColor(theme.primaryContainer)
                        .borderRadius(8.px)
                        .gap(1.cssRem),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpanText(
                        "投稿者: $userId",
                        modifier = Modifier
                            .flexGrow(1)
                            .fontSize(1.1.cssRem)
                            .fontWeight(FontWeight.Medium)
                            .color(theme.onPrimaryContainer)
                    )

                    Button(
                        onClick = { isDialogOpen = true },
                        modifier = Modifier
                            .padding(0.5.cssRem)
                            .backgroundColor(theme.primary)
                            .color(theme.onPrimary)
                    ) {
                        SpanText("変更")
                    }

                    Button(
                        onClick = {
                            UserPreferencesService.clearUserId()
                            userId = null
                            videosResponse = null
                            currentPage = 1
                        },
                        modifier = Modifier
                            .padding(0.5.cssRem)
                            .backgroundColor(theme.error)
                            .color(theme.onError)
                    ) {
                        SpanText("解除")
                    }
                }

                // ローディング表示
                if (isLoading) {
                    SpanText(
                        "読み込み中...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .textAlign(TextAlign.Center)
                            .fontSize(1.1.cssRem)
                            .color(theme.onSurface)
                    )
                }

                // エラーメッセージ
                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(1.cssRem)
                            .borderRadius(8.px)
                            .backgroundColor(theme.errorContainer)
                            .border(1.px, org.jetbrains.compose.web.css.LineStyle.Solid, theme.error)
                    ) {
                        SpanText(
                            error,
                            modifier = Modifier
                                .color(theme.onErrorContainer)
                        )
                    }
                }

                // 動画一覧
                videosResponse?.let { response ->
                    Column(modifier = Modifier.fillMaxWidth().gap(1.5.cssRem)) {
                        // 動画件数表示
                        SpanText(
                            "${response.videosCount}件の動画",
                            modifier = Modifier
                                .fontSize(1.1.cssRem)
                                .fontWeight(FontWeight.Medium)
                                .color(theme.onSurface)
                        )

                        if (response.videos.isEmpty()) {
                            // 空状態
                            SpanText(
                                "動画が見つかりませんでした",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .textAlign(TextAlign.Center)
                                    .padding(2.cssRem)
                                    .fontSize(1.1.cssRem)
                                    .color(theme.onSurfaceVariant)
                            )
                        } else {
                            // 動画グリッド
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .styleModifier {
                                        property("display", "grid")
                                        property("grid-template-columns", "repeat(auto-fill, minmax(250px, 1fr))")
                                        property("gap", "1rem")
                                    }
                            ) {
                                response.videos.forEach { video ->
                                    VideoCard(video)
                                }
                            }

                            // ページネーション
                            Pagination(
                                currentPage = currentPage,
                                hasNext = response.hasNext,
                                onPrevious = {
                                    if (currentPage > 1) {
                                        currentPage--
                                        window.scrollTo(0.0, 0.0)
                                    }
                                },
                                onNext = {
                                    if (response.hasNext) {
                                        currentPage++
                                        window.scrollTo(0.0, 0.0)
                                    }
                                }
                            )
                        }
                    }
                }

                // 別の動画を検索ボタン
                Button(
                    onClick = {
                        UserPreferencesService.clearUserId()
                        userId = null
                        videosResponse = null
                        currentPage = 1
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.75.cssRem)
                        .backgroundColor(theme.surfaceVariant)
                        .color(theme.onSurfaceVariant)
                ) {
                    SpanText("別の動画を直接検索")
                }
            }
        }
    }

    // ユーザーID設定ダイアログ
    UserIdSettingDialog(
        isOpen = isDialogOpen,
        onClose = { isDialogOpen = false },
        onSave = { newUserId ->
            userId = newUserId
            currentPage = 1
            videosResponse = null
        }
    )
}
```

---

## 実装計画

### フェーズ1: バックエンド - モデルとユーティリティ (1-2h)
1. `UserVideo.kt` 作成
2. `UserVideosResponse.kt` 作成
3. `UserIdExtractor.kt` 作成
4. `AtomFeedParser.kt` 作成
5. ユニットテスト作成

### フェーズ2: バックエンド - データソースとキャッシュ (1-2h)
6. `NicoadDataSource.kt` に `fetchUserVideosFeed()` 追加
7. `VideoCacheService.kt` にキャッシュ機能追加
   - `CachedUserVideos` データクラス
   - `UserVideosCacheKey` データクラス
   - `userVideosCache` LoadingCache
   - 関連メソッド

### フェーズ3: バックエンド - APIエンドポイント (30min-1h)
8. `UserVideos.kt` APIエンドポイント作成
9. 手動テスト（Postmanまたはcurl）

### フェーズ4: フロントエンド - ユーティリティとサービス (30min)
10. `UserPreferencesService.kt` 作成

### フェーズ5: フロントエンド - UIコンポーネント (2-3h)
11. `UserIdSettingDialog.kt` 作成
12. `VideoCard.kt` 作成
13. `Pagination.kt` 作成

### フェーズ6: フロントエンド - トップページ (1-2h)
14. `Index.kt` リファクタリング
15. 統合テスト

### フェーズ7: テストと調整 (1-2h)
16. エンドツーエンドテスト
17. エラーケース確認
18. UI/UX調整

**推定合計時間: 7-12時間**

---

## テストケース

### バックエンドテスト

#### UserIdExtractor
- ✅ 直接ID: "92164945" → "92164945"
- ✅ ユーザーページURL: "https://www.nicovideo.jp/user/92164945" → "92164945"
- ✅ 動画ページURL: "https://www.nicovideo.jp/user/92164945/video" → "92164945"
- ✅ RSS URL: "https://www.nicovideo.jp/user/92164945/video?rss=atom" → "92164945"
- ✅ 無効な入力: "abc" → null
- ✅ 空文字: "" → null

#### AtomFeedParser
- ✅ 正常なfeedのパース（複数動画）
- ✅ 空のfeedのパース（0件）
- ✅ 不正なXML（エラーハンドリング）
- ✅ feedUpdatedの取得

#### VideoCacheService
- ✅ 初回アクセス（キャッシュミス）
- ✅ 2回目アクセス（キャッシュヒット）
- ✅ リフレッシュ（キャッシュ削除と再取得）
- ✅ TTL経過後の再取得

#### UserVideos API
- ✅ 正常系: userId=92164945, page=1
- ✅ ページネーション: page=2, page=3
- ✅ 空ページ: page=100
- ✅ エラー: userId未指定（400）
- ✅ エラー: 存在しないユーザー（404）
- ✅ エラー: 無効なpage（400）

### フロントエンドテスト

#### UserPreferencesService
- ✅ setUserId / getUserId
- ✅ clearUserId
- ✅ hasUserId

#### UserIdSettingDialog
- ✅ ダイアログの開閉
- ✅ 有効なIDの保存
- ✅ 無効なIDのエラー表示
- ✅ キャンセル

#### VideoCard
- ✅ 動画情報の表示
- ✅ クリックで広告者ページへ遷移

#### Pagination
- ✅ 1ページ目で「前へ」ボタンが無効
- ✅ 次ページがない場合「次へ」ボタンが無効
- ✅ ページ遷移時のスクロール位置リセット

#### HomePage
- ✅ ユーザーID未設定時の表示
- ✅ ユーザーID設定時の動画一覧表示
- ✅ ページネーション
- ✅ エラーハンドリング
- ✅ ローディング状態

### 統合テスト
- ✅ ユーザーID設定 → 動画一覧表示 → 動画選択 → 広告者ページ表示
- ✅ ページネーションで複数ページを移動
- ✅ ユーザーID変更後の動画一覧更新
- ✅ 存在しないユーザーIDの処理
- ✅ ネットワークエラーの処理

---

## 補足情報

### Caffeineローダー機能の特徴
- キャッシュミス時に自動的にローダー関数が実行される
- スレッドセーフな実装
- 同じキーで複数のリクエストがあっても1回だけロード
- `runBlocking`でsuspend関数を呼び出し可能

### hasNextの判定について
シンプルな判定方法を採用：
- 現在のページに動画があれば `hasNext=true`
- 次のページを実際に取得しないと確実には分からないが、リクエスト削減のためこの方法を採用
- ユーザーが「次へ」を押して0件が返った場合、UI側で適切に処理

### キャッシュ戦略
- ページごとに個別にキャッシュ（`user_videos:92164945:1`）
- TTL: 30分（Atom feedは頻繁に更新されない）
- 最大サイズ: 1000エントリ（既存設定と同じ）
- 統計情報の記録: `recordStats()`

### エラーハンドリング
- 404: ユーザーが存在しない
- 400: 無効なパラメータ
- 500: サーバーエラー
- フロントエンドでは分かりやすいエラーメッセージを表示

---

このドキュメントに基づいて実装を進めてください。
