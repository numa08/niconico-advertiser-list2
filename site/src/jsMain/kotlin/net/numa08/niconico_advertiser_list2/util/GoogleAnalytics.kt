package net.numa08.niconico_advertiser_list2.util

/**
 * Google Analytics 4のイベントトラッキングユーティリティ
 */
object GoogleAnalytics {
    /**
     * ページビューイベントを送信
     * @param pagePath ページパス
     * @param pageTitle ページタイトル
     */
    fun trackPageView(
        pagePath: String,
        pageTitle: String? = null,
    ) {
        if (!isGtagAvailable()) return

        val params = js("{}")
        params["page_path"] = pagePath
        if (pageTitle != null) {
            params["page_title"] = pageTitle
        }

        gtag("event", "page_view", params)
    }

    /**
     * カスタムイベントを送信
     * @param eventName イベント名
     * @param eventParams イベントパラメータ
     */
    fun trackEvent(
        eventName: String,
        eventParams: Map<String, Any> = emptyMap(),
    ) {
        if (!isGtagAvailable()) return

        val params = js("{}")
        eventParams.forEach { (key, value) ->
            params[key] = value
        }

        gtag("event", eventName, params)
    }

    /**
     * 動画検索イベントを送信
     * ユーザーが動画を検索した際に呼び出す
     */
    fun trackVideoSearch() {
        trackEvent("video_search")
    }

    /**
     * ユーザーID設定イベントを送信
     * ユーザーがIDを設定した際に呼び出す
     */
    fun trackUserIdSet() {
        trackEvent("user_id_set")
    }

    /**
     * ユーザーID解除イベントを送信
     * ユーザーがID設定を解除した際に呼び出す
     */
    fun trackUserIdClear() {
        trackEvent("user_id_clear")
    }

    /**
     * gtagが利用可能かチェック
     */
    private fun isGtagAvailable(): Boolean {
        return try {
            js("typeof gtag !== 'undefined'") as Boolean
        } catch (e: Exception) {
            false
        }
    }

    /**
     * gtag関数を呼び出す
     */
    private fun gtag(
        command: String,
        eventName: String,
        params: dynamic,
    ) {
        js("window.gtag")(command, eventName, params)
    }
}
