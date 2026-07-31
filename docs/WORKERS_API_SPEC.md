# Workers バックエンド API 仕様（EARS記法）

Cloudflare Workers 版バックエンド（`workers/`）が満たすべき要件の定義。
[CLOUDFLARE_MIGRATION.md](./CLOUDFLARE_MIGRATION.md) の完了条件を、実装可能な粒度に詳細化したもの。
フェーズ0の実測結果（[PHASE0_SPIKE_RESULTS.md](./PHASE0_SPIKE_RESULTS.md)）と現行JVM実装の挙動を根拠とする。

## EARS記法について

各要件は以下のいずれかのパターンで記述する。

| パターン | 書式 |
|---|---|
| ユビキタス（常時） | システムは、〜しなければならない |
| イベント駆動 | 〜したとき、システムは〜しなければならない |
| 状態駆動 | 〜の間、システムは〜しなければならない |
| 望ましくない挙動 | もし〜の場合、システムは〜しなければならない |
| オプション | 〜を備える場合、システムは〜しなければならない |

主語の「バックエンド」は Workers 上で動作する API 実装を指す。

## 用語

- **ニコニコ**: `www.nicovideo.jp`（watchページ）、`api.koken.nicovideo.jp`（広告履歴API）、`nvapi.nicovideo.jp`（ユーザー動画API）の総称
- **継続カーソル**: 広告履歴を1リクエストで取得しきれない場合に返す `nextOffsetId`。クライアントはこれを `offsetId` パラメータに渡して続きを取得する
- **キャッシュエンベロープ**: KVに保存する `{ payload, cachedAt }` 形式の値

---

## 1. 共通要件（COM）

- **COM-1**（ユビキタス）: バックエンドは、ニコニコへのすべてのHTTPリクエストに Chrome 相当の `User-Agent` ヘッダーを付与しなければならない。
  - 根拠: User-Agent なしでは watchページが503、nvapiが403を返す（フェーズ0実測）
- **COM-2**（ユビキタス）: バックエンドは、nvapi へのリクエストに `X-Frontend-Id: 6` と `X-Frontend-Version: 0` ヘッダーを付与しなければならない。
- **COM-3**（ユビキタス）: バックエンドは、成功レスポンスのJSONに各エンドポイントで定義されたスキーマ以外のフィールドを含めてはならない。
  - 根拠: フロントエンドは `ignoreUnknownKeys` なしの kotlinx.serialization でパースしており、未知フィールドはパースエラーになる
- **COM-4**（望ましくない挙動）: もしニコニコからの取得が失敗した場合（404を除く）、バックエンドは HTTP 500 を返さなければならない。ボディは `/api/video/*` ではJSON `{"error": "..."}`、`/api/user/videos` ではプレーンテキストとする（現行互換）。
  - 補足: 404の除外が意味を持つのは watchページのみ（VI-8で404応答に変換する）。koken API・nvapi は不存在リソースにも200を返すため404は現実には発生しない想定であり（フェーズ0実測）、万一発生した場合は他の失敗と同様に500として扱う
- **COM-5**（ユビキタス）: バックエンドは、シークレットや認証情報をソースコード・設定ファイルにハードコードしてはならない（現時点で必要なシークレットは存在しない）。

## 2. 動画情報 `GET /api/video/info`（VI）

クエリパラメータ: `videoId`（必須）、`refresh`（任意、`true` でキャッシュ無視）

- **VI-1**（イベント駆動）: 有効な `videoId` を受信したとき、バックエンドは watchページ（`https://www.nicovideo.jp/watch/{videoId}`）から動画情報を抽出し、HTTP 200 と以下のスキーマのJSONを返さなければならない。

  ```json
  {
    "videoId": "sm9",
    "title": "動画タイトル",
    "thumbnail": "https://.../thumbnail.jpg",
    "userId": "4",
    "cachedAt": "2026-07-31T00:00:00Z",
    "fromCache": false
  }
  ```

- **VI-2**（ユビキタス）: バックエンドは、`title` を `meta[property="og:title"]` から取得しなければならない。og:title が存在しない場合は `<title>` 要素をフォールバックとし、いずれも無ければ空文字列とする。
- **VI-3**（ユビキタス）: バックエンドは、`thumbnail` を `meta[property="og:image"]` から取得しなければならない。存在しない場合は空文字列とする。
- **VI-4**（ユビキタス）: バックエンドは、`userId` を `script[type="application/ld+json"]` のうち `@type: "VideoObject"` である JSON-LD の `author.url` に含まれる `/user/{数字}` から取得しなければならない。取得できない場合は空文字列とする。
  - 注意: JSON-LD の script タグには他属性（`data-server` 等）が付くため、セレクタは `type` 属性のみで書く。HTML生文では `/user/4` が `\/user\/4` にエスケープされているため、JSONとしてパースしてから抽出する（フェーズ0実測）
- **VI-5**（ユビキタス）: バックエンドは、watchページのHTMLを `HTMLRewriter` によるストリーミング処理で解析しなければならず、HTML全体をメモリ上に文字列として蓄積してはならない（JSON-LDブロック単位のバッファリングは許容する）。
  - 根拠: 無料プランのCPU時間 10ms/リクエスト制限
- **VI-6**（望ましくない挙動）: もし `videoId` が未指定または空の場合、バックエンドは HTTP 400 を返さなければならない（ボディなし、現行互換）。
- **VI-7**（望ましくない挙動）: もし `videoId` に英数字以外の文字が含まれる場合、バックエンドは HTTP 400 を返さなければならない。
  - 現行からの変更（強化）: 任意文字列をURLパスへ連結することによる意図しないエンドポイントへのアクセスを防ぐ
- **VI-8**（望ましくない挙動）: もし watchページが HTTP 404 を返した場合、バックエンドは HTTP 404 とJSON `{"error": "Video not found: {videoId}"}` を返さなければならない。

## 3. 広告履歴 `GET /api/video/nicoad-history`（AH）

クエリパラメータ: `videoId`（必須）、`offsetId`（任意、継続カーソル）、`refresh`（任意）

- **AH-1**（イベント駆動）: 有効な `videoId` を受信したとき、バックエンドは koken API（`https://api.koken.nicovideo.jp/v1/userperspective/contents/nicoad/video/{videoId}/histories?limit=100`）をカーソル方式（前ページ最後の `id` を `offsetId` に渡す）で順次取得し、HTTP 200 と以下のスキーマのJSONを返さなければならない。

  ```json
  {
    "histories": [
      {
        "advertiserName": "広告主名",
        "nicoadId": 12345,
        "adPoint": 100,
        "contribution": 100,
        "startedAt": 1700000000,
        "endedAt": 1700604800,
        "userId": 4,
        "message": null
      }
    ],
    "cachedAt": "2026-07-31T00:00:00Z",
    "fromCache": false,
    "nextOffsetId": 12300
  }
  ```

  - フィールド対応: `advertiserName` ← `supporterName`、`nicoadId` ← `id`、`adPoint` ← `point`、`userId` ← `supporterId`（匿名時 null）。`message` は koken API に対応フィールドがないため常に null
  - `nextOffsetId` は未完了時のみ含める（下記 AH-3）
- **AH-2**（ユビキタス）: バックエンドは、1リクエストで取得する koken API のページ数を最大40に制限しなければならない。
  - 根拠: 無料プランのサブリクエスト上限50回/リクエスト。40ページ + KV読み書き ≤ 42回で余裕を確保
- **AH-3**（状態駆動）: ページ数上限に達してもデータの終端（`nextCount <= 0` または空ページ）に達していない間、バックエンドはレスポンスに継続カーソル `nextOffsetId`（最後に取得した履歴の `id`）を含めなければならない。終端に達した場合は `nextOffsetId` を含めてはならない。
- **AH-4**（イベント駆動）: `offsetId` パラメータ付きのリクエストを受信したとき、バックエンドはそのカーソル位置から取得を開始しなければならない。
- **AH-5**（状態駆動）: koken API のページを連続取得する間、バックエンドはページ間に10〜100msのランダムな遅延を挿入しなければならない（現行互換。遅延はwall-clockでありCPU時間を消費しない）。
- **AH-6**（イベント駆動）: 存在しない動画IDを受信したとき、バックエンドは HTTP 200 と空の `histories` を返さなければならない。
  - 根拠: koken API は不存在動画にも 200 + 空リストを返す（フェーズ0実測）。現行本番も同挙動
- **AH-7**（望ましくない挙動）: もし `videoId` が未指定または空の場合、バックエンドは HTTP 400 を返さなければならない（ボディなし、現行互換）。
- **AH-8**（望ましくない挙動）: もし `videoId` に英数字以外の文字が含まれる場合、または `offsetId` が正の整数でない場合、バックエンドは HTTP 400 を返さなければならない。

## 4. ユーザー動画 `GET /api/user/videos`（UV）

クエリパラメータ: `userId`（必須）、`page`（任意、デフォルト1）、`refresh`（任意）

- **UV-1**（イベント駆動）: 有効な `userId` を受信したとき、バックエンドは nvapi（`https://nvapi.nicovideo.jp/v3/users/{userId}/videos?sortKey=registeredAt&sortOrder=desc&pageSize=30&page={page}`）から動画一覧を取得し、HTTP 200 と以下のスキーマのJSONを返さなければならない。

  ```json
  {
    "userId": "4",
    "page": 1,
    "videos": [
      {
        "videoId": "sm18219289",
        "title": "動画タイトル",
        "thumbnail": "https://.../thumbnail.jpg",
        "published": "2012-07-01T00:00:00+09:00",
        "link": "https://www.nicovideo.jp/watch/sm18219289"
      }
    ],
    "videosCount": 30,
    "hasNext": true,
    "feedUpdated": null,
    "cachedAt": "2026-07-31T00:00:00Z",
    "fromCache": false
  }
  ```

  - フィールド対応: `videoId` ← `items[].essential.id`、`title` ← `essential.title`、`thumbnail` ← `essential.thumbnail.url`、`published` ← `essential.registeredAt`、`link` は `https://www.nicovideo.jp/watch/{videoId}` を組み立てる
  - `feedUpdated` は Atomフィード廃止に伴い対応する値が存在しないため常に null（現行からの変更）
- **UV-2**（ユビキタス）: バックエンドは、`hasNext` を `page × 30 < totalCount` で算出しなければならない。
  - 現行の「次ページ先読み」方式は nvapi の `totalCount` により不要（サブリクエスト1回削減）
- **UV-3**（望ましくない挙動）: もし `userId` が未指定の場合、バックエンドは HTTP 400 とテキスト `Missing required parameter: userId` を返さなければならない（現行互換）。
- **UV-4**（望ましくない挙動）: もし `userId` に数字以外の文字が含まれる場合、バックエンドは HTTP 400 とテキスト `Invalid parameter: userId must be numeric` を返さなければならない（現行互換）。
- **UV-5**（望ましくない挙動）: もし `page` が1未満または整数でない場合、バックエンドは HTTP 400 とテキスト `Invalid parameter: page must be >= 1` を返さなければならない（現行互換）。
- **UV-6**（イベント駆動）: 存在しないユーザーIDを受信したとき、バックエンドは HTTP 200 と空の `videos`（`videosCount: 0`、`hasNext: false`）を返さなければならない。
  - 根拠: nvapi は不存在ユーザーにも 200 + `totalCount: 0` を返す（フェーズ0実測）。「0件」と「不存在」の区別が必要になった場合のみユーザー情報APIの併用を検討する（現行の404応答からの意図的な変更）

## 5. キャッシュ（CA）

- **CA-1**（イベント駆動）: ニコニコからの取得に成功したとき、バックエンドはレスポンスの生成元データをキャッシュエンベロープとしてKVへ書き込まなければならない。TTLは動画情報・広告履歴が1時間、ユーザー動画が30分とする。
- **CA-2**（状態駆動）: 同一キーのキャッシュがTTL内に存在する間、バックエンドはニコニコへアクセスせずKVから応答し、`fromCache: true` と保存時の `cachedAt` を返さなければならない。
- **CA-3**（イベント駆動）: `refresh=true` を受信したとき、バックエンドはキャッシュの有無にかかわらずニコニコから再取得し、キャッシュを更新し、`fromCache: false` を返さなければならない。
- **CA-4**（ユビキタス）: バックエンドは、`fromCache` を実際にキャッシュから応答した場合のみ true にしなければならない。
  - 現行からの変更（修正）: 現行実装は初回取得（キャッシュミス）でも `refresh` 未指定なら `fromCache: true` を返すが、これはバグであり踏襲しない
- **CA-5**（ユビキタス）: バックエンドは、キャッシュキーを以下の単位で構成しなければならない: 動画情報は `videoId`、広告履歴は `videoId` と `offsetId`（先頭ページは固定値）の組、ユーザー動画は `userId` と `page` の組。
- **CA-6**（望ましくない挙動）: もしニコニコからの取得が失敗した場合、バックエンドは失敗結果や不完全なデータをKVへ書き込んではならない。
- **CA-7**（望ましくない挙動）: もしKVの読み書きが失敗した場合、バックエンドはエラーにせず、ニコニコからの直接取得によるレスポンス返却を継続しなければならない（キャッシュはベストエフォート）。

## 6. 非機能要件（NF）

- **NF-1**（ユビキタス）: バックエンドは、1リクエストの処理で発生するサブリクエスト（ニコニコへのfetchとKV操作の合計）を50回未満に抑えなければならない（無料プラン上限）。
- **NF-2**（ユビキタス）: バックエンドは、KVへの書き込みを1リクエストあたり最大1回に抑え、無料プランの書き込み上限（1,000回/日）超過の兆候が見えた場合に備えて書き込み失敗を構造化ログに記録しなければならない。
- **NF-3**（イベント駆動）: 予期しない例外が発生したとき、バックエンドは HTTP 500 を返し、例外内容を構造化ログ（JSON）として出力しなければならない。`passThroughOnException` を使用してはならない。
- **NF-4**（ユビキタス）: バックエンドは、`/api/*` 以外のパスの処理を Workers Static Assets（SPAフォールバック設定）に委ねなければならない。
- **NF-5**（オプション）: 死活監視用エンドポイント `GET /api/health` を備える場合、バックエンドはニコニコへアクセスせずに HTTP 200 と `{"status": "ok"}` を返さなければならない。

## 7. 現行実装からの意図的な変更点（サマリ）

| # | 変更 | 該当要件 | 理由 |
|---|---|---|---|
| 1 | 広告履歴に継続カーソル `nextOffsetId` を追加（1リクエスト最大40ページ） | AH-2, AH-3, AH-4 | 無料プランのサブリクエスト上限対策（案b決定事項） |
| 2 | ユーザー動画を Atomフィード → nvapi へ切り替え | UV-1 | Atomフィード廃止（フェーズ0実測） |
| 3 | `feedUpdated` は常に null | UV-1 | nvapi に相当値がない |
| 4 | 不存在ユーザーは 404 → 200 + 0件 | UV-6 | nvapi が404を返さない。現行本番もフィード廃止により実質機能していない |
| 5 | `hasNext` を先読みではなく `totalCount` から算出 | UV-2 | nvapi で正確に算出可能。サブリクエスト削減 |
| 6 | `fromCache` を実際のキャッシュヒット時のみ true に修正 | CA-4 | 現行実装のバグを踏襲しない |
| 7 | `videoId` / `offsetId` の形式バリデーションを追加 | VI-7, AH-8 | URLパス連結の安全性強化 |
| 8 | グローバル同時実行制限（RequestSemaphore 相当）は導入しない | — | isolate分散環境では再現不可。KVキャッシュで実アクセスが減るため、ページ間遅延の維持のみで許容（移行計画の方針） |

これらはフロントエンド改修（変更点1のフェッチループ・共有モデル更新）およびAPIコントラクトテスト（フェーズ4）の前提となる。

## 8. 自動テスト対応表

各要件と `workers/test/` 配下のテストの相互参照。テスト名は要件IDをプレフィックスに持つ
（例: `it("VI-1: ...")`）。自動テスト不可の要件は確認方法を記載する。

| 要件ID | テストファイル / 確認方法 |
|---|---|
| COM-1 | `video-info.test.ts`, `nicoad-history.test.ts`, `user-videos.test.ts` |
| COM-2 | `user-videos.test.ts` |
| COM-3 | `video-info.test.ts`, `nicoad-history.test.ts`, `user-videos.test.ts`（レスポンスキー集合の厳密一致で検証） |
| COM-4 | `video-info.test.ts`, `nicoad-history.test.ts`, `user-videos.test.ts` |
| COM-5 | 自動テスト対象外（コードレビューで確認。現時点でシークレットは存在しない） |
| VI-1〜VI-4 | `video-info.test.ts` |
| VI-5 | 自動テスト対象外（ストリーミング処理は内部実装制約のためコードレビューで確認） |
| VI-6〜VI-8 | `video-info.test.ts` |
| AH-1〜AH-8 | `nicoad-history.test.ts`（AH-5は経過時間の下限のみ検証） |
| UV-1〜UV-6 | `user-videos.test.ts` |
| CA-1〜CA-7 | `cache.test.ts`（CA-1のTTLはKV listの`expiration`で検証。CA-5はキー粒度を振る舞いで検証） |
| NF-1 | `nicoad-history.test.ts` のAH-2テストで部分検証（最悪ケースのfetch回数=40を確認） |
| NF-2 | 自動テスト対象外（KV書き込み回数は実装レビュー、ログはデプロイ後の観測で確認） |
| NF-3 | `app.test.ts`（onError最終防衛線の500応答と構造化ログのスキーマを検証）、`video-info.test.ts`（fetch例外時の500応答） |
| NF-4 | 自動テスト対象外（wrangler.jsonc の assets 設定。フェーズ3の結合確認で検証） |
| NF-5 | `app.test.ts` |
