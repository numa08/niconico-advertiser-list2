package net.numa08.niconico_advertiser_list2.utils

/**
 * ニコニコ動画のURLから動画IDを抽出するユーティリティ
 */
object VideoIdExtractor {
    /**
     * URLまたは動画IDから動画IDを抽出する
     * @param input ニコニコ動画のURLまたは動画ID
     * @return 動画ID（例: "sm12345678"）
     */
    fun extractVideoId(input: String): String? {
        // TODO: 実際の正規表現でURLから動画IDを抽出する実装に置き換える
        // 現時点ではモック実装として、入力をそのまま返す

        // 空白をトリミング
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return null
        }

        // モック実装: 入力がURL形式でない場合はそのまま動画IDとして扱う
        // 実装例:
        // - https://www.nicovideo.jp/watch/sm12345678 -> sm12345678
        // - https://nico.ms/sm12345678 -> sm12345678
        // - sm12345678 -> sm12345678

        return trimmed
    }
}
