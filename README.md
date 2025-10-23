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
- **広告履歴の取得**: ニコニ広告履歴から広告主リストを取得
- **柔軟なフォーマット設定**:
  - 敬称選択（様、さん、氏、ちゃん、くん、カスタム）
  - 表示順序（すべて表示、逆順、重複除外）
  - 1行あたりの文字数指定
- **クリップボードコピー**: ワンクリックでコピー
- **レスポンシブデザイン**: モバイルにも対応

## 技術スタック

### フロントエンド

- **[Kobweb](https://github.com/varabyte/kobweb) 0.23.3** - Kotlin Multiplatform Web Framework
- **[Compose for Web](https://github.com/JetBrains/compose-multiplatform)** - 宣言的UI
- **[Silk](https://github.com/varabyte/kobweb)** - Kobweb用UIコンポーネント
- **Kotlinx Serialization** - JSON シリアライゼーション

### バックエンド

- **Kotlin/JVM** - サーバーサイドロジック
- **[Kobweb API](https://github.com/varabyte/kobweb)** - サーバーサイドAPIフレームワーク
- **[Ktor Client](https://ktor.io/)** - HTTP クライアント（CIO エンジン）
- **[Ksoup](https://github.com/fleeksoft/ksoup)** - HTML パーサー

### 開発ツール

- **Gradle 8.14** - ビルドシステム
- **Kotlin 2.1.20** - プログラミング言語
- **ktlint** - コードフォーマッター
- **detekt** - 静的解析ツール

### デプロイ

- **Docker** - コンテナ化
- **Koyeb** - PaaS（本番デプロイ先）

## アーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│                   フロントエンド                      │
│  (Compose for Web / Kotlin/JS)                      │
│  - pages/Index.kt: メインページ                       │
│  - VideoIdExtractor: URL/ID抽出ロジック               │
└────────────────┬────────────────────────────────────┘
                 │ HTTP (Fetch API)
                 ↓
┌─────────────────────────────────────────────────────┐
│              バックエンドAPI (Kobweb)                 │
│  (Kotlin/JVM)                                       │
│  - api/VideoInfo.kt: 動画情報API                     │
│  - api/VideoAdvertisers.kt: 広告履歴API              │
│  - util/RequestSemaphore.kt: 同時実行制限             │
└─────────────────────────────────────────────────────┘
```

### データフロー

1. ユーザーが動画ID/URLを入力
2. フロントエンドが`VideoIdExtractor`でIDを抽出
3. バックエンドAPIに並列リクエスト
   - `/api/video/info`: 動画情報取得
   - `/api/video/nicoad-history`: 広告履歴取得
4. `RequestSemaphore`で同時実行数を制限（最大10）
5. レスポンスをフロントエンドで整形して表示

## セットアップ

### 必要な環境

- **Java 21** (JDK)
- **Node.js 20.x** (Kobweb CLI用)
- **Kobweb CLI 0.9.21**

### Kobweb CLIのインストール

```bash
# Linux/macOS
wget https://github.com/varabyte/kobweb-cli/releases/download/v0.9.21/kobweb-0.9.21.zip
unzip kobweb-0.9.21.zip
export PATH=$PATH:$(pwd)/kobweb-0.9.21/bin
```

### プロジェクトのクローン

```bash
git clone https://github.com/numa08/niconico-advertiser-list2.git
cd niconico-advertiser-list2
```

### 開発サーバーの起動

```bash
cd site
kobweb run
```

ブラウザで [http://localhost:8080](http://localhost:8080) を開いてください。

## ビルド

### Gradleビルド

```bash
./gradlew build
```

### 本番ビルド（エクスポート）

```bash
cd site
kobweb export
```

エクスポートされたファイルは `site/.kobweb/` に生成されます。

## テスト

```bash
# すべてのテストを実行
./gradlew test

# JVMテストのみ
./gradlew jvmTest

# JSテストのみ（ブラウザが必要）
./gradlew jsTest
```

## コード品質

```bash
# ktlintチェック
./gradlew ktlintCheck

# ktlint自動修正
./gradlew ktlintFormat

# detekt静的解析
./gradlew detekt
```

## Dockerビルド

```bash
# イメージをビルド
docker build -t niconico-advertiser-list2 .

# コンテナを起動
docker run -p 8080:8080 niconico-advertiser-list2
```

ブラウザで [http://localhost:8080](http://localhost:8080) を開いてください。

## デプロイ

### Koyebへのデプロイ

詳細な手順は [DEPLOYMENT.md](./DEPLOYMENT.md) を参照してください。

**簡易手順**:

1. GitHubにプッシュ
2. [Koyeb](https://www.koyeb.com/) でアカウント作成
3. GitHubリポジトリを連携
4. Dockerfileを使用してデプロイ

**無料プラン**:
- 512MB RAM、0.1 vCPU
- Scale-to-Zero（自動停止/起動）
- カスタムドメイン対応

## プロジェクト構成

```
.
├── site/
│   ├── src/
│   │   ├── commonMain/kotlin/       # 共通コード（フロント/バック）
│   │   │   ├── models/              # データモデル
│   │   │   └── util/                # ユーティリティ
│   │   ├── jsMain/kotlin/           # フロントエンドコード
│   │   │   ├── pages/               # ページコンポーネント
│   │   │   ├── AppEntry.kt          # エントリーポイント
│   │   │   ├── AppStyles.kt         # グローバルスタイル
│   │   │   └── SiteTheme.kt         # テーマ設定
│   │   ├── jvmMain/kotlin/          # バックエンドコード
│   │   │   ├── api/                 # APIエンドポイント
│   │   │   ├── datasource/          # データソース
│   │   │   └── util/                # サーバーユーティリティ
│   │   └── commonTest/kotlin/       # テストコード
│   ├── .kobweb/
│   │   └── conf.yaml                # Kobweb設定
│   └── build.gradle.kts             # ビルド設定
├── gradle/
│   └── libs.versions.toml           # 依存関係バージョン管理
├── Dockerfile                       # Dockerイメージ定義
├── .dockerignore                    # Dockerビルド除外設定
├── DEPLOYMENT.md                    # デプロイメントガイド
└── README.md                        # このファイル
```

## API仕様

### バックエンドAPI

#### GET /api/video/info

動画情報を取得します。

**クエリパラメータ**:
- `videoId`: 動画ID（例: `sm12345678`）

**レスポンス**:
```json
{
  "videoId": "sm12345678",
  "title": "動画タイトル",
  "thumbnail": "https://..."
}
```

#### GET /api/video/nicoad-history

広告履歴を取得します。

**クエリパラメータ**:
- `videoId`: 動画ID（例: `sm12345678`）

**レスポンス**:
```json
[
  {
    "advertiserName": "広告主名",
    "nicoadId": 12345,
    "adPoint": 100,
    "contribution": 100,
    "startedAt": "2024-01-01T00:00:00Z",
    "endedAt": "2024-01-02T00:00:00Z",
    "userId": "12345678",
    "message": "応援メッセージ"
  }
]
```

### レート制限

- **同時実行数**: 最大10リクエスト（グローバルセマフォ）
- 11件目以降は待機状態となり、順次処理されます

## トラブルシューティング

### kobweb runが起動しない

```bash
# Kobweb CLIのバージョン確認
kobweb version

# 期待: v0.9.21
```

### ビルドでメモリ不足

`gradle.properties`でメモリ設定を調整：

```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

### Dockerビルドが遅い

- 初回ビルドは依存関係のダウンロードで5〜10分かかります
- 2回目以降はレイヤーキャッシュが効くため高速です

## ライセンス

MIT License

## 作者

[numa08](https://github.com/numa08)

## 参考リンク

- [Kobweb公式ドキュメント](https://kobweb.varabyte.com/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
