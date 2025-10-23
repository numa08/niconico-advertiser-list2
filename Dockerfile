# =======================================
# Stage 1: ビルド環境
# =======================================
FROM eclipse-temurin:21-jdk AS build

# Kobwebアプリのルートディレクトリ（通常は"site"）
ARG KOBWEB_APP_ROOT="site"
ARG KOBWEB_CLI_VERSION=0.9.21

WORKDIR /project

# Node.jsとnpmのインストール（Playwright用）
RUN apt-get update && \
    apt-get install -y curl wget unzip && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Kobweb CLIのインストール（GitHubからバイナリをダウンロード）
RUN wget https://github.com/varabyte/kobweb-cli/releases/download/v${KOBWEB_CLI_VERSION}/kobweb-${KOBWEB_CLI_VERSION}.zip && \
    unzip kobweb-${KOBWEB_CLI_VERSION}.zip && \
    mv kobweb-${KOBWEB_CLI_VERSION} /opt/kobweb && \
    ln -s /opt/kobweb/bin/kobweb /usr/local/bin/kobweb && \
    rm kobweb-${KOBWEB_CLI_VERSION}.zip

# Playwrightブラウザのインストール（エクスポートに必要）
RUN npx playwright install --with-deps chromium

# 1. Gradle関連ファイルをコピー（依存関係キャッシュ用）
COPY gradle.properties settings.gradle.kts gradlew gradlew.bat ./
COPY gradle/wrapper/ ./gradle/wrapper/
COPY gradle/libs.versions.toml ./gradle/

# 2. ビルドスクリプトとKobweb設定をコピー
COPY ${KOBWEB_APP_ROOT}/build.gradle.kts ./${KOBWEB_APP_ROOT}/
COPY ${KOBWEB_APP_ROOT}/.kobweb/conf.yaml ./${KOBWEB_APP_ROOT}/.kobweb/

# 3. 依存関係をダウンロード（この層はソースコード変更時にキャッシュされる）
RUN chmod +x ./gradlew && \
    ./gradlew dependencies --no-daemon || true

# 4. ソースコードをコピー
COPY ${KOBWEB_APP_ROOT}/src/ ./${KOBWEB_APP_ROOT}/src/

# 5. Gradleビルド実行（ブラウザテストをスキップ）
WORKDIR /project/${KOBWEB_APP_ROOT}
RUN ../gradlew -Dfile.encoding=UTF-8 build -x jsBrowserTest && \
    kobweb export --notty

# =======================================
# Stage 2: 本番環境イメージ
# =======================================
FROM eclipse-temurin:21-jre-jammy

ARG KOBWEB_APP_ROOT="site"

# エクスポートされた成果物のみをコピー
COPY --from=build /project/${KOBWEB_APP_ROOT}/.kobweb /app/.kobweb
COPY --from=build /project/${KOBWEB_APP_ROOT}/build /app/build

WORKDIR /app

# ポート8080を公開
EXPOSE 8080

# サーバー起動スクリプトを実行
ENTRYPOINT ["/app/.kobweb/server/start.sh"]
