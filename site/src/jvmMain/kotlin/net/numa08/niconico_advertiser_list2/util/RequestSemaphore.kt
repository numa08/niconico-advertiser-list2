package net.numa08.niconico_advertiser_list2.util

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * グローバルセマフォによるリクエスト制限
 *
 * 悪意のある第三者による大量のAPI呼び出しを防ぐため、
 * 同時に処理できるリクエスト数を制限します。
 */
object RequestSemaphore {
    /**
     * 同時実行可能なリクエスト数
     * この値を超えるリクエストは待機状態になります
     */
    private const val MAX_CONCURRENT_REQUESTS = 10

    private val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    /**
     * セマフォで保護された処理を実行する
     *
     * @param block 実行する処理
     * @return 処理の結果
     */
    suspend fun <T> withLimit(block: suspend () -> T): T =
        semaphore.withPermit {
            block()
        }
}
