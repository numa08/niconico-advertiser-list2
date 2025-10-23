# Koyebへのデプロイメントガイド

このドキュメントでは、niconico-advertiser-list2アプリケーションをKoyebにデプロイする手順を説明します。

## 前提条件

- GitHubアカウント
- Koyebアカウント（無料プラン利用可能）
- このリポジトリがGitHubにプッシュされていること

## デプロイ手順

### 1. GitHubにプッシュ

```bash
git add Dockerfile .dockerignore DEPLOYMENT.md
git commit -m "Add Koyeb deployment configuration"
git push origin main
```

### 2. Koyebアカウントの作成

1. [Koyeb](https://www.koyeb.com/)にアクセス
2. GitHubアカウントで登録（推奨）
3. Hobbyプラン（無料）を選択

### 3. アプリケーションのデプロイ

#### 3.1 新しいサービスを作成

1. Koyebダッシュボードで「Create Web Service」をクリック
2. 「GitHub」を選択
3. リポジトリを認可（初回のみ）
4. `niconico-advertiser-list2`リポジトリを選択

#### 3.2 ビルド設定

- **Builder**: `Dockerfile`を選択
- **Dockerfile path**: `Dockerfile`（デフォルト）
- **Branch**: `main`または`master`
- **Build context**: プロジェクトルート（デフォルト）

#### 3.3 インスタンス設定

- **Instance type**: `Eco`（無料プラン: 512MB RAM, 0.1 vCPU）
- **Scaling**: `1`（Hobbyプランでは固定）
- **Regions**: `Frankfurt (fra)`または`Washington (was)`

#### 3.4 環境設定

- **Port**: `8080`
- **Protocol**: `HTTP`
- **Health check path**: `/`（オプション）

#### 3.5 環境変数（必要に応じて）

現時点では特別な環境変数は不要ですが、将来的に以下を設定可能：
- `JAVA_OPTS`: JVMオプション（例: `-Xmx400m`）

#### 3.6 デプロイ実行

「Deploy」ボタンをクリックしてデプロイを開始します。

### 4. デプロイの確認

- 初回ビルドは**5〜10分**かかります（Gradle依存関係のダウンロードとビルド）
- ビルドログで進行状況を確認できます
- デプロイ完了後、Koyebが提供するURLでアプリケーションにアクセス可能
  - 例: `https://your-app-name-xyz123.koyeb.app`

### 5. カスタムドメインの設定（オプション）

Starterプラン以上で、カスタムドメインを設定可能：

1. Koyebダッシュボードで「Domains」をクリック
2. 「Add custom domain」を選択
3. DNSプロバイダーでCNAMEレコードを設定
4. SSL証明書は自動発行

## トラブルシューティング

### ビルドが失敗する場合

1. **メモリ不足**:
   - `gradle.properties`に以下を追加:
     ```
     org.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m
     ```

2. **Playwrightのインストールに失敗**:
   - Dockerfileで`--with-deps`オプションが正しく設定されているか確認

3. **タイムアウト**:
   - Koyebのビルドタイムアウトは30分（通常は十分）
   - 初回ビルドは依存関係のダウンロードで時間がかかります

### アプリケーションが起動しない場合

1. **ポート設定を確認**:
   - `site/.kobweb/conf.yaml`で`server.port: 8080`を確認
   - Koyeb設定で同じポートを指定

2. **ログを確認**:
   - Koyebダッシュボードの「Logs」タブでエラーを確認

### Scale-to-Zero（自動停止）

- Hobbyプランでは、トラフィックがない場合に自動停止します
- 次のリクエストで自動起動（コールドスタート: 数秒〜10秒）
- これは無料プランの正常な動作です

## CORS設定（必要な場合）

異なるドメインからAPIを呼び出す場合、`site/.kobweb/conf.yaml`にCORS設定を追加：

```yaml
server:
  port: 8080
  cors:
    hosts:
      - name: "your-app-name.koyeb.app"
        schemes:
          - "https"
      - name: "localhost"
        schemes:
          - "http"
```

変更後、GitHubにプッシュすると自動的に再デプロイされます。

## 継続的デプロイメント（CD）

Koyebは`main`ブランチへのプッシュを検知し、自動的に再デプロイします：

1. コードを変更
2. GitHubにプッシュ
3. Koyebが自動的にビルド＆デプロイ

## コスト

- **Hobbyプラン**: 完全無料
  - 1つのWeb Service（512MB RAM、0.1 vCPU）
  - 1つのDatabase（50時間/月）
  - Scale-to-Zero強制

- アプリケーションが無料枠を超えることはありません

## 参考リンク

- [Koyeb公式ドキュメント](https://www.koyeb.com/docs)
- [Kobweb公式ドキュメント](https://kobweb.varabyte.com/docs)
- [Kobweb Exporting](https://kobweb.varabyte.com/docs/concepts/foundation/exporting)
