# workers/ — Cloudflare Workers 移行版バックエンド

Koyeb（Kobweb JVMサーバー）からの移行先。API（`/api/*`）を TypeScript + Hono で再実装し、
Kobwebフロントエンドの静的エクスポート成果物を Workers Static Assets として配信する。
全体計画は [docs/CLOUDFLARE_MIGRATION.md](../docs/CLOUDFLARE_MIGRATION.md) を参照。

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

初回のみ KV namespace を作成し、`wrangler.jsonc` の `id` を差し替える:

```bash
pnpm wrangler kv namespace create NICO_CACHE
```

```bash
pnpm deploy
```

本番デプロイは GitHub Actions からの `wrangler deploy` に集約する（フェーズ5で整備）。
