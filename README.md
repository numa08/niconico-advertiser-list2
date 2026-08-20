# ニコニコ動画 広告主リスト取得ツール

ニコニコ動画の広告履歴から広告主リストを取得し、整形して表示するWebアプリケーションです。

## 概要

このツールは、ニコニコ動画の動画IDまたはURLを入力することで：

- 動画情報（タイトル、サムネイル）を取得
- ニコニ広告の履歴を取得
- 広告主の名前リストをカスタマイズ可能な形式で表示
- クリップボードへのコピー機能

動画投稿者が広告をくださった方々への感謝を表明する際に便利です。

## 主な機能

- **動画情報の取得**: 動画ID/URLから動画情報を表示
- **広告履歴の取得**: ニコニ広告履歴から広告主リストを取得（継続カーソル方式で大量広告にも対応）
- **ユーザー投稿動画一覧**: ユーザーIDを設定すると自分の投稿動画から選択可能
- **柔軟なフォーマット設定**:
  - 敬称選択（様、さん、氏、ちゃん、くん、カスタム）
  - 区切り文字選択（読点「、」、句点「。」、カスタム）
  - 表示順序（すべて表示、逆順、重複除外）
  - 1行あたりの文字数指定
- **クリップボードコピー**: ワンクリックでコピー
- **レスポンシブデザイン**: モバイルにも対応

## 技術スタック

### バックエンド（`workers/`）

- **[Cloudflare Workers](https://workers.cloudflare.com/)** - エッジサーバーレスランタイム
- **[Hono](https://hono.dev/)** - ルーター
- **TypeScript** - 実装言語
- **Workers KV** - キャッシュ（動画情報・広告履歴: TTL 1時間、ユーザー動画: 30分）
- **HTMLRewriter** - watchページのストリーミングHTMLパース

### フロントエンド（`frontend/`）

- **[React](https://react.dev/)** + **[React Router](https://reactrouter.com/)** - UI
- **[Vite](https://vite.dev/)** - ビルドツール
- **TypeScript** - 実装言語
- ビルド成果物は Workers Static Assets として同一Workerから配信（SPAフォールバック有効）

### 開発ツール

- **pnpm workspace** - mono-repo管理（`workers/` + `frontend/`）
- **Vitest** - テスト（workersは `@cloudflare/vitest-pool-workers` でworkerd上実行）
- **Biome** - リンター/フォーマッター
- **Wrangler** - Cloudflare開発・デプロイCLI

### デプロイ

- **Cloudflare Workers Builds** - gitリポジトリ連携による自動ビルド・デプロイ

## アーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│              フロントエンド (frontend/)               │
│  React + React Router (Vite ビルド)                  │
│  - 動画検索 / 広告主リスト表示 / フォーマット設定       │
│  - Workers Static Assets として配信                  │
└────────────────┬────────────────────────────────────┘
                 │ HTTP (fetch, 相対パス /api/*)
                 ↓
┌─────────────────────────────────────────────────────┐
│           バックエンドAPI (workers/)                  │
│  Cloudflare Workers + Hono (TypeScript)             │
│  - /api/video/info: 動画情報取得                     │
│  - /api/video/nicoad-history: 広告履歴取得           │
│  - /api/user/videos: ユーザー投稿動画取得             │
│  - Workers KV: TTL付きキャッシュ                     │
└─────────────────────────────────────────────────────┘
```

## セットアップ

### 必要な環境

- **Node.js 20.x以上**
- **pnpm**（バージョンは `package.json` の `packageManager` を参照）

### インストール

```bash
git clone https://github.com/numa08/niconico-advertiser-list2.git
cd niconico-advertiser-list2
pnpm install
```

### 開発サーバーの起動

```bash
# バックエンド（wrangler dev、http://localhost:8787）
pnpm dev

# フロントエンド（Vite devサーバー。/api を localhost:8787 へプロキシ）
pnpm -C frontend dev
```

詳細は [workers/README.md](./workers/README.md) を参照してください。

## ビルド・テスト

```bash
# 全パッケージの型チェック
pnpm typecheck

# 全パッケージのテスト
pnpm test

# フロントエンドのビルド（frontend/dist/ に生成）
pnpm -C frontend build

# CI相当（typecheck + 全テスト + フロントエンドビルド）
pnpm ci:build

# リント / フォーマット
pnpm lint
pnpm format
```

## デプロイ

本番デプロイは **Cloudflare Workers Builds**（gitリポジトリ連携）で自動化されています。
mainブランチへのpushでビルド・デプロイが実行されます。

設定値・手動デプロイ手順は [workers/README.md](./workers/README.md) を参照してください。

## プロジェクト構成

```
.
├── workers/                 # バックエンド（Cloudflare Workers + Hono）
│   ├── src/
│   │   ├── app.ts           # ルーティング・エラーハンドリング
│   │   ├── cache.ts         # Workers KVキャッシュ層
│   │   └── nico/            # ニコニコ動画アクセス（watchページ/koken API/nvapi）
│   ├── test/                # Vitest（workerd上で実行）
│   ├── scripts/             # APIコントラクトテスト等
│   └── wrangler.jsonc       # Workers設定（KV・Static Assets）
├── frontend/                # フロントエンド（React + Vite）
│   ├── src/
│   │   ├── pages/           # ページコンポーネント
│   │   ├── components/      # UIコンポーネント
│   │   ├── lib/             # ロジック（ID抽出・フォーマット・APIクライアント）
│   │   └── context/         # 設定などの共有状態
│   └── test/                # Vitest
├── docs/                    # 仕様・移行ドキュメント
│   ├── CLOUDFLARE_MIGRATION.md   # Cloudflare移行計画
│   ├── WORKERS_API_SPEC.md       # API仕様（EARS記法）
│   └── FRONTEND_REQUIREMENTS.md  # フロントエンド要件（EARS記法）
└── package.json             # pnpm workspaceルート
```

## API仕様

詳細な仕様（EARS記法）は [docs/WORKERS_API_SPEC.md](./docs/WORKERS_API_SPEC.md) を参照してください。

### GET /api/video/info

動画情報を取得します。

**クエリパラメータ**:
- `videoId`: 動画ID（例: `sm12345678`）
- `refresh`: `true` でキャッシュを無視して再取得（オプション）

### GET /api/video/nicoad-history

広告履歴を取得します。1リクエストで最大40ページ（4,000件）を取得し、
終端に達していない場合は継続カーソル `nextOffsetId` を返します。

**クエリパラメータ**:
- `videoId`: 動画ID（例: `sm12345678`）
- `offsetId`: 継続カーソル（前回レスポンスの `nextOffsetId`。オプション）
- `refresh`: `true` でキャッシュを無視して再取得（オプション）

### GET /api/user/videos

ユーザーの投稿動画一覧を取得します。

**クエリパラメータ**:
- `userId`: ユーザーID（数字のみ）
- `page`: ページ番号（1以上。オプション、デフォルト1）
- `refresh`: `true` でキャッシュを無視して再取得（オプション）

## ライセンス

MIT License

## 作者

[numa08](https://github.com/numa08)

## 参考リンク

- [Cloudflare Workers公式ドキュメント](https://developers.cloudflare.com/workers/)
- [Hono公式ドキュメント](https://hono.dev/)
- [React公式ドキュメント](https://react.dev/)
