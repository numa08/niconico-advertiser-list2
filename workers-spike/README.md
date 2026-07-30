# フェーズ0検証用Worker

Cloudflareエッジ（本物のWorkersエグレスIP）からニコニコの各エンドポイントへ
アクセスできるかを最終確認するためのスパイクです。**本番コードではありません。**
検証が完了したらWorkerごと削除して構いません。

サンドボックスおよびローカルworkerd（`wrangler dev`）での検証は通過済みです。
残るはCloudflareの実エグレスIPがニコニコ側にブロックされないかの確認のみです。

## デプロイと実行

```bash
cd workers-spike
npx wrangler login          # 初回のみ（ブラウザでCloudflareアカウントを認可）
npx wrangler deploy
```

デプロイ後に表示されるURL（`https://niconico-advertiser-list-spike.<account>.workers.dev`）へアクセス:

### 1. 基本チェック

```bash
curl https://niconico-advertiser-list-spike.<account>.workers.dev/
```

期待値（すべて満たせばフェーズ0のエッジ検証は合格）:

```jsonc
{
  "watchPage":    { "status": 200, "ogTitle": "新・豪血寺一族...", "ogImage": "https://...", "authorUserId": "4" },
  "watchPage404": { "status": 404 },
  "nicoadApi":    { "status": 200, "itemCount": 100, "cursorFollowStatus": 200 },
  "nvapi":        { "status": 200, "totalCount": 19 }
}
```

### 2. ページング実測（サブリクエスト上限の確認）

```bash
curl "https://niconico-advertiser-list-spike.<account>.workers.dev/paginate?videoId=sm9&maxPages=45"
```

- 無料プランはサブリクエスト50回/リクエストのため `maxPages=45` で安全側に検証
- `maxPages=60` などにすると `stoppedBy: "exception: Too many subrequests"` で
  上限挙動そのものを観測できる

## 後片付け

```bash
npx wrangler delete
```
