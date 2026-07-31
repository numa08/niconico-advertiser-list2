# workers/ — Cloudflare Workers 移行版バックエンド

API（`/api/*`）を TypeScript + Hono で実装し、Reactフロントエンド（`../frontend`）の
ビルド成果物を Workers Static Assets として配信する。
移行の経緯は [docs/CLOUDFLARE_MIGRATION.md](../docs/CLOUDFLARE_MIGRATION.md) を参照。

## 技術スタック

- **ランタイム**: Cloudflare Workers（無料プラン前提。広告履歴は継続カーソル方式でサブリクエスト上限50に対応）
- **ルーター**: [Hono](https://hono.dev/)
- **キャッシュ**: Workers KV（バインディング名 `NICO_CACHE`）
- **テスト**: Vitest + `@cloudflare/vitest-pool-workers`（workerd上でテスト実行）
- **静的配信**: Workers Static Assets（`../frontend/dist`、SPAフォールバック有効）

## セットアップ

リポジトリはpnpm workspaceのmono-repo構成（`workers/` + `frontend/`）。
依存のインストールはリポジトリルートで行う。

```bash
pnpm install          # リポジトリルートで実行
cd workers
pnpm cf-typegen       # worker-configuration.d.ts（Env型）を生成
```

## 開発

```bash
pnpm dev          # wrangler dev（アセットディレクトリが無ければ空で作成される）
pnpm test         # Vitest（workerd上で実行。ネットワークアクセスなし）
pnpm typecheck    # tsc --noEmit
```

APIコントラクトテスト（現行Koyeb本番との互換性検証。ネットワークアクセスあり・手動実行）:

```bash
pnpm dev                # 別ターミナルでWorkersを起動しておく
pnpm test:contract      # LEGACY_BASE / NEW_BASE 環境変数で対象URLを変更可能
```

フロントエンドも含めて動作確認する場合は、先に `pnpm -C ../frontend build`
（`frontend/dist/` に成果物が生成される）を実行してから `pnpm dev` を起動する。
フロントエンド開発時は `pnpm -C ../frontend dev`（Vite devサーバー）が
`/api` を `localhost:8787` へプロキシするため、両方を起動して開発できる。

## デプロイ

本番デプロイは **Cloudflare Workers Builds**（gitリポジトリ連携）で自動化する。
mainブランチへのpushでビルド・デプロイが実行され、それ以外のブランチは
`wrangler versions upload` によるプレビューになる。

ダッシュボードでの設定値（Workers & Pages → Create → Import a repository、
または既存Workerの Settings → Builds → Connect）:

| 項目 | 値 |
|---|---|
| リポジトリ / 本番ブランチ | `numa08/niconico-advertiser-list2` / `main` |
| Root directory | `/`（リポジトリルート。pnpm workspaceのため） |
| Build command | `pnpm install --frozen-lockfile && pnpm ci:build` |
| Deploy command | `pnpm -C workers deploy` |
| 非本番ブランチのDeploy command | `pnpm -C workers exec wrangler versions upload` |
| ビルド環境変数 | `GA4_MEASUREMENT_ID`（GA4測定ID。フロントエンドビルド時に注入） |

- `pnpm ci:build` はルートpackage.jsonのスクリプトで、typecheck → 全テスト（workers + frontend）→
  フロントエンドビルドを実行する。**テスト失敗時はデプロイが中断される**
- Worker名はダッシュボードと `wrangler.jsonc` の `name`（`niconico-advertiser-list2`）が一致している必要がある
- KVバインディング `NICO_CACHE` の本番namespaceは作成済み（IDは `wrangler.jsonc` に記載）

手元からの手動デプロイも可能:

```bash
pnpm -C frontend build && pnpm -C workers deploy
```
