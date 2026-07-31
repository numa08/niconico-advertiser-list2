// 対応仕様: docs/WORKERS_API_SPEC.md
//   VI-1〜VI-4, VI-6〜VI-8（VI-5はストリーミング処理の実装制約のためコードレビューで確認）
//   COM-1, COM-3, COM-4（/api/video/info 分）, NF-3（一部: fetch例外時の500応答）
import { env } from "cloudflare:workers";
import { afterEach, describe, expect, it, vi } from "vitest";
import { app } from "../src/app";
import { WATCH_ORIGIN, expectJson, mockNicoFetch, watchPageHtml } from "./helpers";

afterEach(() => {
  vi.restoreAllMocks();
});

function watchRoute(videoId: string, html: string) {
  return {
    origin: WATCH_ORIGIN,
    path: `/watch/${videoId}`,
    handler: () =>
      new Response(html, { headers: { "content-type": "text/html; charset=utf-8" } }),
  };
}

describe("GET /api/video/info", () => {
  it("VI-1: 有効なvideoIdで200と動画情報スキーマのJSONを返す", async () => {
    mockNicoFetch([
      watchRoute(
        "sm100001",
        watchPageHtml({
          ogTitle: "テスト動画タイトル",
          ogImage: "https://img.example/thumb.jpg",
          jsonLdAuthorUrl: "https://www.nicovideo.jp/user/4",
        }),
      ),
    ]);

    const res = await app.request("/api/video/info?videoId=sm100001", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as Record<string, unknown>;
    expect(body.videoId).toBe("sm100001");
    expect(body.title).toBe("テスト動画タイトル");
    expect(body.thumbnail).toBe("https://img.example/thumb.jpg");
    expect(body.userId).toBe("4");
    expect(typeof body.cachedAt).toBe("string");
    expect(Number.isNaN(Date.parse(body.cachedAt as string))).toBe(false);
    expect(typeof body.fromCache).toBe("boolean");
  });

  it("COM-3: レスポンスに定義済みスキーマ以外のフィールドを含めない", async () => {
    mockNicoFetch([
      watchRoute(
        "sm100002",
        watchPageHtml({
          ogTitle: "t",
          ogImage: "https://img.example/t.jpg",
          jsonLdAuthorUrl: "https://www.nicovideo.jp/user/4",
        }),
      ),
    ]);

    const res = await app.request("/api/video/info?videoId=sm100002", {}, env);
    const body = await expectJson(res);
    expect(Object.keys(body).sort()).toEqual(
      ["cachedAt", "fromCache", "thumbnail", "title", "userId", "videoId"].sort(),
    );
  });

  it("COM-1: watchページへのリクエストにChrome相当のUser-Agentを付与する", async () => {
    const recorded = mockNicoFetch([
      watchRoute("sm100003", watchPageHtml({ ogTitle: "t" })),
    ]);

    await app.request("/api/video/info?videoId=sm100003", {}, env);
    const watchRequests = recorded.filter((r) => r.url.origin === WATCH_ORIGIN);
    expect(watchRequests.length).toBeGreaterThan(0);
    for (const r of watchRequests) {
      expect(r.headers.get("user-agent")).toMatch(/Chrome/);
    }
  });

  it("VI-2: og:titleが無い場合はtitle要素へフォールバックする", async () => {
    mockNicoFetch([
      watchRoute("sm100004", watchPageHtml({ titleTag: "フォールバックタイトル" })),
    ]);

    const res = await app.request("/api/video/info?videoId=sm100004", {}, env);
    const body = await expectJson(res);
    expect(body.title).toBe("フォールバックタイトル");
  });

  it("VI-3: og:imageが無い場合はthumbnailを空文字列にする", async () => {
    mockNicoFetch([watchRoute("sm100005", watchPageHtml({ ogTitle: "t" }))]);

    const res = await app.request("/api/video/info?videoId=sm100005", {}, env);
    const body = await expectJson(res);
    expect(body.thumbnail).toBe("");
  });

  it("VI-4: JSON-LD(VideoObject)のauthor.urlからuserIdを抽出する", async () => {
    mockNicoFetch([
      watchRoute(
        "sm100006",
        watchPageHtml({
          ogTitle: "t",
          jsonLdAuthorUrl: "https://www.nicovideo.jp/user/753685",
        }),
      ),
    ]);

    const res = await app.request("/api/video/info?videoId=sm100006", {}, env);
    const body = await expectJson(res);
    expect(body.userId).toBe("753685");
  });

  it("VI-4: JSON-LDからuserIdを取得できない場合は空文字列にする", async () => {
    mockNicoFetch([watchRoute("sm100007", watchPageHtml({ ogTitle: "t" }))]);

    const res = await app.request("/api/video/info?videoId=sm100007", {}, env);
    const body = await expectJson(res);
    expect(body.userId).toBe("");
  });

  it("VI-6: videoId未指定は400を返す", async () => {
    mockNicoFetch([]);
    const res = await app.request("/api/video/info", {}, env);
    expect(res.status).toBe(400);
  });

  it("VI-6: videoIdが空文字列は400を返す", async () => {
    mockNicoFetch([]);
    const res = await app.request("/api/video/info?videoId=", {}, env);
    expect(res.status).toBe(400);
  });

  it("VI-7: videoIdに英数字以外が含まれる場合は400を返し、ニコニコへアクセスしない", async () => {
    const recorded = mockNicoFetch([]);
    const res = await app.request(
      `/api/video/info?videoId=${encodeURIComponent("../evil/path")}`,
      {},
      env,
    );
    expect(res.status).toBe(400);
    expect(recorded.length).toBe(0);
  });

  it("VI-8: watchページが404の場合は404とerrorボディを返す", async () => {
    mockNicoFetch([
      {
        origin: WATCH_ORIGIN,
        path: "/watch/sm999999999999",
        handler: () => new Response("not found", { status: 404 }),
      },
    ]);

    const res = await app.request("/api/video/info?videoId=sm999999999999", {}, env);
    const body = await expectJson(res, 404);
    expect(body.error).toBe("Video not found: sm999999999999");
  });

  it("COM-4: watchページが404以外のエラーの場合は500とerrorボディを返す", async () => {
    mockNicoFetch([
      {
        origin: WATCH_ORIGIN,
        path: "/watch/sm100008",
        handler: () => new Response("server error", { status: 503 }),
      },
    ]);

    const res = await app.request("/api/video/info?videoId=sm100008", {}, env);
    expect(res.status).toBe(500);
    const body = (await res.json()) as Record<string, unknown>;
    expect(typeof body.error).toBe("string");
  });

  it("NF-3: fetchが例外を投げた場合は500とerrorボディを返す", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async () => {
      throw new TypeError("Network connection lost");
    });

    const res = await app.request("/api/video/info?videoId=sm100009", {}, env);
    expect(res.status).toBe(500);
    const body = (await res.json()) as Record<string, unknown>;
    expect(typeof body.error).toBe("string");
  });
});
