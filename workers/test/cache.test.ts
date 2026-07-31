// 対応仕様: docs/WORKERS_API_SPEC.md
//   CA-1〜CA-7（キャッシュ要件）
//   注: CA-5のキー粒度は内部実装に踏み込まず「キャッシュが別単位で管理される」振る舞いで検証する
import { reset } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { afterEach, describe, expect, it, vi } from "vitest";
import { app } from "../src/app";
import {
  KOKEN_ORIGIN,
  NVAPI_ORIGIN,
  WATCH_ORIGIN,
  expectJson,
  kokenRoute,
  mockNicoFetch,
  nvapiItem,
  nvapiResponse,
  watchPageHtml,
} from "./helpers";

afterEach(async () => {
  vi.restoreAllMocks();
  // vitest 4版pool-workersはテスト間のストレージ自動分離を行わないため、KVを明示的に初期化する
  await reset();
});

const VIDEO_INFO_TTL_SECONDS = 3600;
const USER_VIDEOS_TTL_SECONDS = 1800;

function watchRoute(videoId: string) {
  return {
    origin: WATCH_ORIGIN,
    path: `/watch/${videoId}`,
    handler: () =>
      new Response(
        watchPageHtml({
          ogTitle: "キャッシュテスト動画",
          ogImage: "https://img.example/thumb.jpg",
          jsonLdAuthorUrl: "https://www.nicovideo.jp/user/4",
        }),
        { headers: { "content-type": "text/html; charset=utf-8" } },
      ),
  };
}

function nvapiRoute(userId: string) {
  return {
    origin: NVAPI_ORIGIN,
    path: `/v3/users/${userId}/videos`,
    handler: () => Response.json(nvapiResponse(1, [nvapiItem("sm1", "t")])),
  };
}

describe("キャッシュ", () => {
  it("CA-1: 取得成功時にKVへTTL付きで書き込む（動画情報: 1時間）", async () => {
    mockNicoFetch([watchRoute("sm400001")]);

    await app.request("/api/video/info?videoId=sm400001", {}, env);
    const list = await env.NICO_CACHE.list();
    expect(list.keys.length).toBe(1);
    const expiration = list.keys[0]!.expiration;
    expect(expiration).toBeDefined();
    const nowSeconds = Date.now() / 1000;
    expect(expiration!).toBeGreaterThanOrEqual(nowSeconds + VIDEO_INFO_TTL_SECONDS - 120);
    expect(expiration!).toBeLessThanOrEqual(nowSeconds + VIDEO_INFO_TTL_SECONDS + 120);
  });

  it("CA-1: 取得成功時にKVへTTL付きで書き込む（ユーザー動画: 30分）", async () => {
    mockNicoFetch([nvapiRoute("400002")]);

    await app.request("/api/user/videos?userId=400002", {}, env);
    const list = await env.NICO_CACHE.list();
    expect(list.keys.length).toBe(1);
    const expiration = list.keys[0]!.expiration;
    expect(expiration).toBeDefined();
    const nowSeconds = Date.now() / 1000;
    expect(expiration!).toBeGreaterThanOrEqual(nowSeconds + USER_VIDEOS_TTL_SECONDS - 120);
    expect(expiration!).toBeLessThanOrEqual(nowSeconds + USER_VIDEOS_TTL_SECONDS + 120);
  });

  it("CA-2/CA-4: 初回はfromCache=false、TTL内の2回目はニコニコへ再アクセスせずfromCache=trueで応答する", async () => {
    const recorded = mockNicoFetch([watchRoute("sm400003")]);

    const first = await expectJson(
      await app.request("/api/video/info?videoId=sm400003", {}, env),
    );
    expect(first.fromCache).toBe(false);
    const fetchCountAfterFirst = recorded.length;
    expect(fetchCountAfterFirst).toBeGreaterThan(0);

    const second = await expectJson(
      await app.request("/api/video/info?videoId=sm400003", {}, env),
    );
    expect(second.fromCache).toBe(true);
    // ニコニコへの再アクセスなし
    expect(recorded.length).toBe(fetchCountAfterFirst);
    // cachedAtは保存時の値を返す
    expect(second.cachedAt).toBe(first.cachedAt);
  });

  it("CA-3: refresh=trueはキャッシュがあってもニコニコから再取得しfromCache=falseを返す", async () => {
    const recorded = mockNicoFetch([watchRoute("sm400004")]);

    await app.request("/api/video/info?videoId=sm400004", {}, env);
    const fetchCountAfterFirst = recorded.length;

    const refreshed = await expectJson(
      await app.request("/api/video/info?videoId=sm400004&refresh=true", {}, env),
    );
    expect(refreshed.fromCache).toBe(false);
    expect(recorded.length).toBeGreaterThan(fetchCountAfterFirst);

    // 再取得後もキャッシュは更新されている（3回目はキャッシュヒット）
    const third = await expectJson(
      await app.request("/api/video/info?videoId=sm400004", {}, env),
    );
    expect(third.fromCache).toBe(true);
  });

  it("CA-5: 広告履歴のキャッシュは(videoId, offsetId)単位で管理される", async () => {
    const recorded = mockNicoFetch([kokenRoute("sm400005", 4100)]);

    // 先頭チャンク（40ページ）を取得してキャッシュ
    const first = (await expectJson(
      await app.request("/api/video/nicoad-history?videoId=sm400005", {}, env),
    )) as { nextOffsetId?: number };
    const fetchCountAfterFirst = recorded.length;
    expect(first.nextOffsetId).toBeDefined();

    // 同一カーソル（先頭）はキャッシュヒットし、fetchが増えない
    await app.request("/api/video/nicoad-history?videoId=sm400005", {}, env);
    expect(recorded.length).toBe(fetchCountAfterFirst);

    // 継続カーソル指定は別チャンクなので新たにfetchが発生する
    const res = await app.request(
      `/api/video/nicoad-history?videoId=sm400005&offsetId=${first.nextOffsetId}`,
      {},
      env,
    );
    expect(res.status).toBe(200);
    expect(recorded.length).toBeGreaterThan(fetchCountAfterFirst);
  });

  it("CA-5: ユーザー動画のキャッシュは(userId, page)単位で管理される", async () => {
    const recorded = mockNicoFetch([nvapiRoute("400006")]);

    await app.request("/api/user/videos?userId=400006&page=1", {}, env);
    const fetchCountAfterFirst = recorded.length;

    // 別ページは別キャッシュ単位なので新たにfetchが発生する
    await app.request("/api/user/videos?userId=400006&page=2", {}, env);
    expect(recorded.length).toBeGreaterThan(fetchCountAfterFirst);
  });

  it("CA-6: 取得失敗時はKVへ書き込まない", async () => {
    mockNicoFetch([
      {
        origin: WATCH_ORIGIN,
        handler: () => new Response("server error", { status: 503 }),
      },
    ]);

    const res = await app.request("/api/video/info?videoId=sm400007", {}, env);
    expect(res.status).toBe(500);
    const list = await env.NICO_CACHE.list();
    expect(list.keys.length).toBe(0);
  });

  it("CA-7: KVが故障していてもニコニコからの直接取得で応答する", async () => {
    mockNicoFetch([watchRoute("sm400008")]);
    const brokenKv = {
      get: () => {
        throw new Error("KV read failure");
      },
      put: () => {
        throw new Error("KV write failure");
      },
      list: () => {
        throw new Error("KV list failure");
      },
      delete: () => {
        throw new Error("KV delete failure");
      },
      getWithMetadata: () => {
        throw new Error("KV read failure");
      },
    } as unknown as KVNamespace;
    const brokenEnv = { ...env, NICO_CACHE: brokenKv };

    const res = await app.request("/api/video/info?videoId=sm400008", {}, brokenEnv);
    expect(res.status).toBe(200);
    const body = (await res.json()) as Record<string, unknown>;
    expect(body.title).toBe("キャッシュテスト動画");
    expect(body.fromCache).toBe(false);
  });
});
