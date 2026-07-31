# Cloudflare 移行計画

Koyeb（Docker + JVM 常駐サーバー）から Cloudflare への移行に向けた、動作環境の選定・タスク整理・完了条件の定義。

## 1. 現状の整理

### バックエンドが実際にやっていること

| エンドポイント | 処理内容 | 外部アクセス先 |
|---|---|---|
| `GET /api/video/info` | watchページのHTMLを取得し、metaタグ/JSON-LDからタイトル・サムネイル・投稿者IDを抽出 | `https://www.nicovideo.jp/watch/{videoId}` |
| `GET /api/video/nicoad-history` | 広告履歴をカーソル方式（offsetId、100件/ページ）で全ページ取得し1リストに結合。ページ間に10〜100msのランダム遅延 | `https://api.koken.nicovideo.jp/v1/userperspective/contents/nicoad/video/{videoId}/histories` |
| `GET /api/user/videos` | ユーザー投稿動画のAtomフィードを取得しXMLパース。次ページ先読みで `hasNext` 判定 | ニコニコ動画ユーザーAtomフィード |

補助機構:

- **キャッシュ**: Caffeine（メモリ内）。動画情報・広告履歴はTTL 1時間、ユーザー動画は30分、最大1000件。`cachedAt` / `fromCache` をレスポンスに含める。`refresh=true` で強制再取得
- **同時実行制限**: `RequestSemaphore` でニコニコへの同時アクセスを最大10に制限
- **フロントエンド**: Kobweb（Compose for Web / Kotlin/JS）。ビルド後は静的アセット（JSバンドル + HTML）であり、API呼び出しは相対パス `/api/*` の fetch のみ
- **GA4**: ビルド時に測定IDを埋め込み

つまり「外部HTTP取得 + パース + 整形 + 短命キャッシュ」だけであり、JVM・コンテナ・常駐プロセスは本質的に不要。**エッジのサーバーレス関数で十分**である。

## 2. 適した動作環境（推奨構成）

### 推奨: Cloudflare Workers（Static Assets 同梱の単一 Worker）

| 要素 | 採用技術 | 現行の置き換え対象 |
|---|---|---|
| API | Cloudflare Workers + TypeScript（ルーターは [Hono](https://hono.dev/) を推奨） | Kobweb API（Kotlin/JVM + Ktor） |
| HTMLパース | Workers 組み込みの `HTMLRewriter`（ストリーミング処理でCPU時間を節約） | Ksoup |
| XMLパース | 軽量XMLパーサ（`fast-xml-parser` 等） | Ksoup |
| キャッシュ | Workers KV（TTL付き書き込み、`cachedAt` メタデータを値に含めて保存） | Caffeine |
| 静的配信 | Workers Static Assets（`wrangler.toml` の `assets` 設定） | Kobweb サーバーの静的配信 |
| デプロイ | `wrangler deploy`（GitHub Actions から実行） | Docker ビルド + Koyeb |

- Cloudflare Pages ではなく **Workers（Static Assets）** を選ぶ。Pages は機能的に Workers へ統合が進んでおり、新規構築は Workers が公式推奨
- JVM は Workers 上で動かないため**バックエンドの TypeScript 再実装は必須**（Cloudflare Containers なら現行イメージも動くが、有料かつ「過剰」の解消にならないため不採用）

### フロントエンドの扱い（2案）

- **案A（推奨・第1段階）**: Kobweb フロントエンドを `kobweb export --layout static` で静的エクスポートし、そのまま Workers Static Assets として配信する。API は相対パス fetch なので変更不要。Gradle + Playwright のビルドチェーンは CI 内にのみ残る
- **案B（任意・第2段階）**: フロントエンドも TypeScript（Vite + 任意の軽量フレームワーク）で再実装し、Kotlin/Gradle/Playwright を完全に排除してツールチェーンを Node.js に一本化する

案Aで先に Koyeb から退去し、案Bは必要になったときに別プロジェクトとして判断するのが低リスク。

### 制約・リスク（設計時に織り込むこと）

1. **サブリクエスト上限**: Workers 無料プランは1リクエストあたり外部fetch 50回まで。広告履歴は100件/ページなので、**約5,000件を超える広告が付いた動画**で頭打ちになる。対策: (a) Workers Paid（$5/月、1000回）にする、(b) 上限到達時は部分結果とカーソルを返しクライアントが続きを要求する設計にする — いずれかを移行時に決定する
2. **CPU時間**: 無料プランは10ms/リクエスト。watchページのHTMLは大きいため、DOM全体を構築せず `HTMLRewriter` でストリーミング抽出する（ランダム遅延などの待ち時間はwall-clockでありCPU時間を消費しないので問題ない）
3. **グローバル同時実行制限の消失**: `RequestSemaphore`（全体で同時10）は isolate 単位で分散する Workers ではそのまま再現できない。KVキャッシュでニコニコへの実アクセスが大きく減ることを踏まえ、まずはページ間遅延の維持のみで許容する。厳密な全体制御が必要になったら Durable Objects で導入する
4. **KV書き込み上限**: 無料プランは1,000書き込み/日（= 新規キャッシュ対象1,000件/日）。超過が見えたら Cache API 併用または Paid 化
5. **ニコニコ側のアクセス制御**: Cloudflare のエグレスIPがニコニコ側にブロックされないかは**移行前に実測検証（スパイク）が必須**

## 3. 移植タスク

### フェーズ0: 事前検証（スパイク）

検証結果の詳細は [PHASE0_SPIKE_RESULTS.md](./PHASE0_SPIKE_RESULTS.md) を参照。

- [x] 3つの外部エンドポイントへのアクセス検証（サンドボックス + ローカルworkerd）
      → watchページ・koken APIはOK（**User-Agent必須**）。**Atomフィードは廃止されており nvapi へ移行が必要**
- [x] `kobweb export --layout static` の検証 → 成立。ただし動的ルート `/advertisers/{videoId}` はSPAフォールバック（`not_found_handling: "single-page-application"`）で対応する
- [x] 広告件数の多い実動画でのページ数実態調査 → sm9で8,000件超を確認。無料プランのサブリクエスト上限50では不足
- [x] `workers-spike/` をCloudflareアカウントへデプロイし、実エッジIPからの疎通を最終確認する → 全項目期待値どおり（ブロックなし）
- [x] サブリクエスト上限の対策（案a: Workers Paid化 / 案b: 継続カーソル方式）を決定する
      → **案b（継続カーソル方式）に決定**（2026-07-31）。無料枠を維持する。サーバーは1リクエストあたり最大約40ページを集約し、未完了の場合はレスポンスに継続カーソル（`nextOffsetId`）を追加フィールドで返す。フロントエンドは完了まで繰り返し呼ぶ（Kobweb側の共有モデル更新とフェッチループの改修が必要）

### フェーズ1: プロジェクト基盤

- [x] `workers/` ディレクトリに wrangler + TypeScript + Hono + Vitest のプロジェクトを作成する
      → pnpm管理。vitest 4 + `@cloudflare/vitest-pool-workers` 0.19（`cloudflareTest()` プラグイン形式）でworkerd上のテストが動作
- [x] `wrangler.jsonc` に KV namespace・Static Assets・環境変数を定義する
      → KVバインディング `NICO_CACHE`（本番namespace IDはデプロイ前に `wrangler kv namespace create` で作成して差し替え）、Static Assets は `../site/.kobweb/site` + SPAフォールバック。GA4測定IDはランタイムではなくKobwebビルド時に注入するためフェーズ3で扱う
- [x] ローカル開発フロー（`wrangler dev` + 静的アセット）を整備する
      → `pnpm dev` で起動し `/api/health` の応答を確認済み。手順は `workers/README.md` を参照

### フェーズ2: バックエンド再実装（TypeScript）

詳細な要件定義（EARS記法）は [WORKERS_API_SPEC.md](./WORKERS_API_SPEC.md) を参照。

- [x] `GET /api/video/info`: watchページ取得 + `HTMLRewriter` によるメタ情報抽出（タイトル・サムネイル・userId、JSON-LDフォールバック含む）→ `workers/src/nico/watchPage.ts`
- [x] `GET /api/video/nicoad-history`: koken API のカーソルページング取得、ページ間ランダム遅延（10〜100ms）の維持。案bにより1リクエスト最大40ページで打ち切り、未完了時は `nextOffsetId`（継続カーソル）を返す。`offsetId` クエリパラメータで続きから取得できる → `workers/src/nico/nicoad.ts`
- [x] `GET /api/user/videos`: **nvapi**（`nvapi.nicovideo.jp/v3/users/{id}/videos`、`X-Frontend-Id: 6` 必須）からの取得に切り替える（Atomフィードは廃止済み）。`totalCount` から `hasNext` を算出、userId/page のバリデーション（数値のみ・page>=1）→ `workers/src/nico/nvapi.ts`
- [x] KVキャッシュ層: TTL（動画情報・広告履歴1時間、ユーザー動画30分）、`cachedAt`/`fromCache` の付与、`refresh=true` での強制再取得 → `workers/src/cache.ts`
- [x] エラー互換: 400（パラメータ不正）/ 404（動画不存在、`{"error": ...}` ボディ）/ 500 を現行と同じ形式で返す（ユーザー不存在は仕様変更で200+0件。WORKERS_API_SPEC.md セクション7参照）→ `workers/src/app.ts`

フェーズ2はEARS仕様（WORKERS_API_SPEC.md）ベースのTDDで実装。単体テスト44件（`workers/test/`）がすべてグリーン。

### フェーズ3: フロントエンド静的化

- [x] `kobweb export --layout static` の成果物を Workers Static Assets として配信する
      → `wrangler dev` で配信確認済み（index.html / JSバンドル / favicon）
- [x] フロントエンドの継続カーソル対応（案bの前提）: 共有モデルに `nextOffsetId` を追加し、`fetchNicoadHistory` がカーソルを完了までたどる方式に改修。sm9（広告23,775件=238ページ=6チャンク）の実データで結合表示を確認済み
- [x] GA4 測定IDの注入 → 既存の `GA4_MEASUREMENT_ID` 環境変数によるビルド時注入（`site/build.gradle.kts`）が静的エクスポートでもそのまま機能する。CIワークフローへの環境変数設定はフェーズ5で行う
- [x] SPAルーティングの動作を確認する
      → ナビゲーションリクエストはプラットフォームのSPAフォールバックで `index.html` が返る。`Sec-Fetch-Mode` を送らないクライアントはWorkerに到達するため、`app.notFound` から `ASSETS` バインディングへ委譲するフォールバックを実装（`/api/*` 以外）。`/advertisers/sm9` 直アクセス・未知パス・`/api/unknown`(404) をローカルで確認済み

### フェーズ4: テスト

- [ ] 既存テスト相当の移植: `VideoIdExtractor` / `UserIdExtractor` / `AtomFeedParser` のテストケースを TypeScript 側パーサに対して移植する
- [ ] APIコントラクトテスト: 現行 Koyeb 環境と新 Workers 環境に同一リクエストを投げ、レスポンスJSONの互換性を検証する
- [ ] キャッシュ挙動（TTL内2回目で `fromCache=true`、`refresh=true` で再取得）のテスト

### フェーズ5: CI/CD

- [ ] GitHub Actions で「Kobweb 静的エクスポート → Workers ビルド → `wrangler deploy`」を行うワークフローを作成する
- [ ] PR 時のプレビュー環境（`wrangler versions upload` / preview URL）を設定する

### フェーズ6: 切り替え・撤収

- [ ] 本番URL（カスタムドメイン or `*.workers.dev`）を決定し、DNS/公開URLを切り替える
- [ ] 並行稼働期間中に GA4 計測・主要動画IDでの動作を確認する
- [ ] Koyeb のサービスを停止・削除する
- [ ] README / DEPLOYMENT.md を Cloudflare 前提に更新し、Dockerfile 等の不要ファイルを整理する

## 4. 移植の完了条件

記法: 「◯◯は◯◯されたとき、◯◯しなければならない」

### API互換性

1. `/api/video/info` は、有効な `videoId` が指定されたとき、HTTP 200 と動画情報（videoId・タイトル・サムネイルURL・投稿者ID・cachedAt・fromCache）のJSONを現行と同一のスキーマで返さなければならない。
2. `/api/video/info` は、`videoId` が未指定または空で呼び出されたとき、HTTP 400 を返さなければならない。
3. `/api/video/info` は、存在しない動画IDが指定されたとき、HTTP 404 と `{"error": "..."}` 形式のJSONを返さなければならない。
4. `/api/video/nicoad-history` は、広告履歴が複数ページ（100件超）存在する動画IDが指定されたとき、全ページを取得し1つの履歴リストに結合して返さなければならない。
5. `/api/video/nicoad-history` は、広告履歴のページを連続取得するとき、ページ間に10〜100msの遅延を挿入しなければならない。
6. `/api/user/videos` は、`userId` に数字以外が含まれるとき、または `page` が1未満のとき、HTTP 400 を返さなければならない。
7. `/api/user/videos` は、有効な `userId` が指定されたとき、動画リスト・件数・`hasNext`・`feedUpdated`・`cachedAt`・`fromCache` を含むJSONを現行と同一のスキーマで返さなければならない。
8. バックエンドは、ニコニコ側への取得が失敗したとき（404以外）、HTTP 500 と `{"error": "..."}` 形式のJSONを返さなければならない。

### キャッシュ

9. バックエンドは、同一 `videoId` の動画情報リクエストがTTL（1時間）内に再度到着したとき、ニコニコ側へ再アクセスせずキャッシュから応答し、`fromCache=true` を返さなければならない。
10. バックエンドは、`refresh=true` が指定されたとき、キャッシュの有無にかかわらずニコニコ側から再取得し、キャッシュを更新しなければならない。
11. キャッシュは、TTL（動画情報・広告履歴: 1時間、ユーザー動画: 30分）が経過したとき、次回リクエストで再取得されなければならない。

### フロントエンド

12. ユーザーが有効な動画ID/URLを入力して検索したとき、フロントエンドは動画情報と広告主リストを表示しなければならない。
13. ユーザーがフォーマット設定（敬称・表示順序・1行文字数）を変更したとき、フロントエンドは表示中の広告主リストへ即座に反映しなければならない。
14. ユーザーがコピーボタンを押下したとき、フロントエンドは整形済みリストをクリップボードへコピーしなければならない。
15. ブラウザが `/advertisers` などのページURLへ直接アクセスしたとき、Workers は対応するページを返さなければならない（HTTP 404 の誤配信をしてはならない）。
16. ページが読み込まれたとき、GA4測定タグが現行と同様に送信されなければならない。

### 非機能・運用

17. 新環境は、アイドル状態からリクエストを受けたとき、コールドスタートによる数秒超の待ち時間を発生させてはならない（現行 Koyeb の Scale-to-Zero 起因の遅延を解消しなければならない）。
18. 新環境は、現行水準のトラフィックを受けたとき、Cloudflare 無料枠（またはあらかじめ合意した Paid プラン）の範囲内で稼働しなければならない。
19. CI は、`master` ブランチへ変更がプッシュされたとき、自動でビルドと `wrangler deploy` を実行しなければならない。
20. CI は、テスト（パーサ単体テスト・APIコントラクトテスト）が失敗したとき、デプロイを中断しなければならない。

### 切り替え・撤収

21. 公開URLは、切り替えが実施されたとき、新環境（Workers）へ到達しなければならない。
22. 新環境での安定稼働が確認されたとき、Koyeb 上のサービスは停止・削除されなければならない。
23. 移行が完了したとき、README および DEPLOYMENT.md は Cloudflare 前提の手順に更新されなければならない。
