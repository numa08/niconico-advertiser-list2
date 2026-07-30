# フェーズ0: スパイク検証結果

実施日: 2026-07-30
実施環境: クラウドサンドボックス（データセンターIPからの直接アクセス）+ ローカルworkerd（`wrangler dev`、Workersと同一ランタイム）

> **注意**: Cloudflareの実エグレスIPからの最終確認は未実施。`workers-spike/` を
> `npx wrangler deploy` してエッジから同じチェックを実行すること（手順は
> `workers-spike/README.md`）。ここまでの検証はすべてデータセンターIPで
> ブロックされていないため、エッジでも通る見込みは高い。

## 結果サマリ

| 検証項目 | 結果 | 備考 |
|---|---|---|
| watchページ取得（`www.nicovideo.jp/watch/{id}`） | ✅ | **User-Agent必須**（下記） |
| watchページからのメタ情報抽出（HTMLRewriter） | ✅ | og:title / og:image / JSON-LD userId すべて抽出成功 |
| koken広告API（`api.koken.nicovideo.jp`） | ✅ | カーソルページング動作確認済み |
| ユーザー動画Atomフィード | ❌ **廃止されている** | 代替のnvapiを検証済み（下記） |
| nvapi（`nvapi.nicovideo.jp/v3/users/{id}/videos`） | ✅ | `X-Frontend-Id: 6` + User-Agent 必須 |
| レート制限・IPブロック | ✅ 遭遇せず | 60ms間隔で80連続リクエストしても正常応答 |
| `kobweb export --layout static` | ⏳ 検証中 | ビルド実行中。完了後にこの表と本文を更新する |

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

### 6. `kobweb export --layout static`（検証中）

静的レイアウトでのエクスポートを実行中（Kotlin/JSプロダクションビルドに
時間がかかる）。完了後、`site/.kobweb/site/` に `index.html` /
`/advertisers` 用ページ / 404ページ / JSバンドルが生成されるかを確認し、
本セクションを結果で更新する。

成功した場合のフェーズ3方針:

- この成果物をそのままWorkers Static Assetsのディレクトリとして配信する
- `/advertisers` へのアクセスに対応するHTMLを返すルーティング
  （Static Assetsの `html_handling` デフォルト挙動で対応可能）を確認する

## フェーズ0の残タスク

- [ ] `workers-spike/` をCloudflareアカウントへデプロイし、実エッジIPからの
      疎通を確認する（`/` と `/paginate?videoId=sm9&maxPages=45`）
- [ ] サブリクエスト上限対策（案a: Paid化 / 案b: 継続カーソル方式）の決定
- [ ] （推奨）Koyeb本番の `/api/user/videos` が現在も動作しているか確認する
      （Atomフィード廃止の影響確認）
