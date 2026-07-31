# フェーズ0: スパイク検証結果

実施日: 2026-07-30〜31
実施環境: クラウドサンドボックス（データセンターIPからの直接アクセス）+ ローカルworkerd（`wrangler dev`、Workersと同一ランタイム）+ **Cloudflare実エッジ（`workers-spike/` をデプロイして確認済み）**

## 結果サマリ

| 検証項目 | 結果 | 備考 |
|---|---|---|
| watchページ取得（`www.nicovideo.jp/watch/{id}`） | ✅ | **User-Agent必須**（下記） |
| watchページからのメタ情報抽出（HTMLRewriter） | ✅ | og:title / og:image / JSON-LD userId すべて抽出成功 |
| koken広告API（`api.koken.nicovideo.jp`） | ✅ | カーソルページング動作確認済み |
| ユーザー動画Atomフィード | ❌ **廃止されている** | 代替のnvapiを検証済み（下記） |
| nvapi（`nvapi.nicovideo.jp/v3/users/{id}/videos`） | ✅ | `X-Frontend-Id: 6` + User-Agent 必須 |
| レート制限・IPブロック | ✅ 遭遇せず | 60ms間隔で80連続リクエストしても正常応答 |
| **Cloudflare実エッジからの疎通** | ✅ | デプロイした `workers-spike/` で全項目が期待値どおり（下記） |
| `kobweb export --layout static` | ✅ | 成果物生成を確認。ただし動的ルートはSPAフォールバック必須（本文参照） |

## 詳細

### 1. User-Agentがないとbot対策に引っかかる

workerdのfetchはデフォルトでUser-Agentヘッダーを送らない。その状態では:

- watchページ: 存在しない動画へのアクセスが 404 ではなく **503** になる（bot向け応答）
- nvapi: **403** が返る

Chrome相当のUser-Agentを付与するとすべて正常化した。
**本実装ではニコニコへの全リクエストにUser-Agentを明示すること。**

### 2. koken広告APIの挙動（本実装で踏襲すべき仕様）

- レスポンス: `{"meta":{"status":200},"data":{"nextCount":N,"totalPoint":N,"histories":[...]}}`
- カーソル方式: 前ページ最後の `id` を `offsetId` に渡す。`nextCount <= 0` で終端
- **存在しない動画IDでもHTTP 200**（`histories: []`, `totalPoint: 0`）を返す。
  404は返さない。つまり現行JVM実装の「koken 404 → VideoNotFoundException」は
  実際には発火せず、現行の `/api/video/nicoad-history` は不存在動画に対して
  「200 + 空リスト」を返している。新実装もこの挙動を踏襲する
  （動画の存在チェックはwatchページ側が404を返すのでそちらで判定できる）

### 3. ページング深度の実測 — サブリクエスト上限問題は現実

sm9（総広告ポイント約2,331万）で実測:

- **80ページ（8,000件）取得しても終端に達しない**（テスト側の上限で中断）
- 80連続リクエスト・60ms間隔で約57秒。ブロック・レート制限なし

Workers無料プランのサブリクエスト上限は50回/リクエストのため、
**sm9級の動画は無料プランでは1リクエストで取得しきれない**（上限約4,900件）。
対策はフェーズ2の設計判断として以下のいずれかを選ぶ:

- **案a: Workers Paid（$5/月）** — 上限1,000回（=約10万件）。実装は現行と同じ全件集約のまま
- **案b: 継続カーソル方式** — サーバーは最大約45ページを集約して部分結果と
  継続カーソルを返し、フロントエンドが完了まで繰り返し呼ぶ。無料枠のままだが
  APIコントラクトとフロントエンドの改修が必要

### 4. ユーザー動画Atomフィードは廃止済み → nvapiへ移行が必要

`https://www.nicovideo.jp/user/{id}/video?rss=atom&page={n}` は、動画を持つ
アクティブユーザーに対しても **HTMLページを返す**（Atom XMLを返さない。
`rss=2.0` も同様）。ヘッダーを本番同等（Accept: application/atom+xml、
Chrome UA）にしても結果は変わらない。

**つまり現行本番の `/api/user/videos` はすでに機能していない可能性が高い**
（フィードのXMLパース結果が常に0件になる）。Koyeb環境での現状確認を推奨。

代替: ニコニコWeb本体が使用しているnvapi。

```
GET https://nvapi.nicovideo.jp/v3/users/{userId}/videos?sortKey=registeredAt&sortOrder=desc&pageSize=30&page={n}
X-Frontend-Id: 6
X-Frontend-Version: 0
User-Agent: （必須）
```

- レスポンス: `{"meta":{"status":200},"data":{"totalCount":N,"items":[{"essential":{"id","title","registeredAt","thumbnail":{...},...}}]}}`
- `totalCount` が取れるため、現行実装の「次ページ先読みによるhasNext判定」が不要になる（1リクエスト削減）
- `X-Frontend-Id` なしは400、User-Agentなしは403
- **存在しないユーザーIDでも200 + `totalCount: 0`** が返る（404にならない）。
  現行APIの「ユーザー不存在→404」を維持するかは仕様判断（「0件」と「不存在」の
  区別が必要なら別途ユーザー情報APIの併用を検討）

### 5. HTMLRewriterでのメタ情報抽出は成立する

Workers組み込みのHTMLRewriter（ストリーミングパーサ）で以下すべての抽出に成功:

- `meta[property="og:title"]` → タイトル
- `meta[property="og:image"]` → サムネイルURL
- `script[type="application/ld+json"]`（`@type: VideoObject` の `author.url`）→ 投稿者userId

注意点:

- JSON-LDの `<script>` タグには `data-server="1"` など他属性が付くため、
  セレクタは `type` 属性のみで書く
- HTML中の生テキストでは `/user/4` が `\/user\/4` とエスケープされているため、
  現行JVM実装のフォールバック（HTML全体への正規表現）は素のパターンでは
  一致しない。JSON-LDをJSONとしてパースしてから抽出するのが正
- 検証はローカルworkerd（`wrangler dev`）で実施済み。CPU 10ms制限内で
  49KBのwatchページを処理できた（ストリーミングのためDOM構築コストなし）

### 6. `kobweb export --layout static` は成立する（SPAフォールバック必須）

静的レイアウトでのエクスポートに成功（ビルド約9分）。`site/.kobweb/site/` に
以下が生成された（合計約2.5MB）:

- `index.html`（`/` のプリレンダリング）
- `404.html`
- `niconico_advertiser_list2.js`（+ ソースマップ）
- `favicon.ico` ほか静的リソース

**重要**: 広告主リストページは `@Page("/advertisers/{videoId}")` の**動的ルート**
のため、静的エクスポートではHTMLがプリレンダリングされない（スナップショット
対象は `/` と `/404` のみ）。現行はKobweb JVMサーバーが動的ルートへシェルHTMLを
返しているが、静的配信では代わりに **Workers Static Assetsの
`not_found_handling: "single-page-application"`** を設定し、未知パスに
`index.html` を返してクライアント側のKobwebルーターに処理させる。

フェーズ3の設定方針（`wrangler.jsonc`）:

```jsonc
{
  "assets": {
    "directory": "../site/.kobweb/site",
    "not_found_handling": "single-page-application"
  }
}
```

付随する仕様変化: 存在しないパスへの直アクセスはHTTP 200 + クライアント側
NotFoundページ表示となる（現行の404ステータス応答から変わる）。SEO上の懸念が
あれば `run_worker_first` でWorker側ルーティングを挟む余地はあるが、本アプリの
性質上は許容範囲と判断してよい。

補足: エクスポートにはPlaywright管理のChromium headless shellが必要
（今回の環境では playwright build v1187 が自動ダウンロードされた）。CI設計時は
Dockerfile同様にブラウザ取得手段を確保すること。

### 7. Cloudflare実エッジからの疎通確認（2026-07-31実施）

`workers-spike/` を実際のCloudflareアカウントへデプロイし、エッジの `GET /` で
全項目が期待値どおりであることを確認した:

```json
{
  "checkedAt": "2026-07-30T23:55:20.082Z",
  "watchPage": {"status": 200, "ogTitle": "新・豪血寺一族 -煩悩解放 - レッツゴー！陰陽師", "authorUserId": "4"},
  "watchPage404": {"status": 404},
  "nicoadApi": {"status": 200, "itemCount": 100, "nextCount": 100, "totalPoint": 23318300, "cursorFollowStatus": 200, "cursorFollowItemCount": 100},
  "nvapi": {"status": 200, "totalCount": 19, "itemCount": 19, "firstVideoId": "sm18219289"}
}
```

（og:image も正常に取得。上記は主要フィールドの抜粋）

**結論: CloudflareのエグレスIPはニコニコのbot対策にブロックされていない。**
watchページのメタ抽出（HTMLRewriter）、koken APIのカーソルページング、
nvapiのすべてが実エッジ環境で動作する。技術的なブロッカーはなく、移行は成立する。

## フェーズ0の残タスク

- [x] `workers-spike/` をCloudflareアカウントへデプロイし、実エッジIPからの
      疎通を確認する → ✅ 全項目期待値どおり（上記セクション7）
- [x] サブリクエスト上限対策（案a: Paid化 / 案b: 継続カーソル方式）の決定
      → **案b（継続カーソル方式）に決定**（2026-07-31、詳細は CLOUDFLARE_MIGRATION.md フェーズ0）
- [x] （推奨）Koyeb本番の `/api/user/videos` が現在も動作しているか確認する
      → 2026-07-31確認: **機能していない**。動画19件保有のuser 4に対し200 + 0件を返す
      （Atomフィード廃止の影響。フェーズ4のコントラクトテストで実証。nvapi移行で修復される）
- [ ] 検証完了後、スパイクWorkerを削除する（`cd workers-spike && npx wrangler delete`）
