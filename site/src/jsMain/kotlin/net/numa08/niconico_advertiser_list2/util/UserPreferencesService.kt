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
    fun getUserId(): String? = window.localStorage.getItem(USER_ID_KEY)

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
    fun hasUserId(): Boolean = getUserId() != null
}
