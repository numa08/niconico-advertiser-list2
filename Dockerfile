# =======================================
# Stage 1: ビルド環境
# =======================================
# ディストリビューションを未固定にするとPlaywrightが対応しないOSに更新されて
# エクスポートが失敗するため、ランタイムと同じjammy(22.04)に固定する
FROM eclipse-temurin:21-jdk-jammy AS build

# Kobwebアプリのルートディレクトリ（通常は"site"）
ARG KOBWEB_APP_ROOT="site"
ARG KOBWEB_CLI_VERSION=0.9.21
# kobweb exportが内部で使うplaywright-javaのバージョンと一致させる必要がある
# （Kobwebプラグイン更新時は要追従。不一致だとChromiumのリビジョンが合わず起動できない）
ARG PLAYWRIGHT_VERSION=1.55.0
ARG GA4_MEASUREMENT_ID

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
RUN npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium

# 1. Gradle関連ファイルをコピー（依存関係キャッシュ用）
COPY gradle.properties settings.gradle.kts gradlew gradlew.bat gradle-ci.properties ./
COPY gradle/wrapper/ ./gradle/wrapper/
COPY gradle/libs.versions.toml ./gradle/

# 2. ビルドスクリプトとKobweb設定をコピー
COPY ${KOBWEB_APP_ROOT}/build.gradle.kts ./${KOBWEB_APP_ROOT}/
COPY ${KOBWEB_APP_ROOT}/.kobweb/conf.yaml ./${KOBWEB_APP_ROOT}/.kobweb/

# 3. 依存関係をダウンロード（この層はソースコード変更時にキャッシュされる）
RUN chmod +x ./gradlew -Dorg.gradle.project.file=gradle-ci.properties && \
    ./gradlew -Dorg.gradle.project.file=gradle-ci.properties dependencies --no-daemon || true

# 4. ソースコードをコピー
COPY ${KOBWEB_APP_ROOT}/src/ ./${KOBWEB_APP_ROOT}/src/

# 5. Gradleビルド実行（ブラウザテストをスキップ）
WORKDIR /project/${KOBWEB_APP_ROOT}
# kobweb export --nottyは失敗しても終了コード0を返すことがあるため、
# 成果物の存在を検証してサイレント失敗を防ぐ
RUN export GA4_MEASUREMENT_ID="${GA4_MEASUREMENT_ID}" && \
    ../gradlew -Dorg.gradle.project.file=gradle-ci.properties -Dfile.encoding=UTF-8 build -x jsBrowserTest && \
    kobweb export --notty && \
    if [ -z "$(ls -A .kobweb/site)" ]; then \
        echo "ERROR: kobweb export failed (.kobweb/site is empty)" >&2; \
        exit 1; \
    fi

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
