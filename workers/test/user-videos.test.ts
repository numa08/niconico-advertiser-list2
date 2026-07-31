// 対応仕様: docs/WORKERS_API_SPEC.md
//   UV-1〜UV-6, COM-1, COM-2, COM-3, COM-4（/api/user/videos 分）
import { env } from "cloudflare:workers";
import { afterEach, describe, expect, it, vi } from "vitest";
import { app } from "../src/app";
import {
  NVAPI_ORIGIN,
  expectJson,
  mockNicoFetch,
  nvapiItem,
  nvapiResponse,
} from "./helpers";

afterEach(() => {
  vi.restoreAllMocks();
});

function nvapiRoute(userId: string, totalCount: number, items: ReturnType<typeof nvapiItem>[]) {
  return {
    origin: NVAPI_ORIGIN,
    path: `/v3/users/${userId}/videos`,
    handler: () => Response.json(nvapiResponse(totalCount, items)),
  };
}

describe("GET /api/user/videos", () => {
  it("UV-1: 有効なuserIdで200とユーザー動画スキーマのJSONを返す", async () => {
    mockNicoFetch([
      nvapiRoute("300001", 2, [
        nvapiItem("sm18219289", "動画1"),
        nvapiItem("sm18219290", "動画2"),
      ]),
    ]);

    const res = await app.request("/api/user/videos?userId=300001", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as Record<string, unknown>;
    expect(body.userId).toBe("300001");
    expect(body.page).toBe(1);
    expect(body.videosCount).toBe(2);
    expect(body.hasNext).toBe(false);
    expect(body.feedUpdated).toBeNull();
    expect(typeof body.cachedAt).toBe("string");
    expect(typeof body.fromCache).toBe("boolean");

    const video = (body.videos as Record<string, unknown>[])[0]!;
    expect(video.videoId).toBe("sm18219289");
    expect(video.title).toBe("動画1");
    expect(video.thumbnail).toBe(
      "https://nicovideo.cdn.nimg.jp/thumbnails/sm18219289/default.jpg",
    );
    expect(video.published).toBe("2024-01-15T12:34:56+09:00");
    expect(video.link).toBe("https://www.nicovideo.jp/watch/sm18219289");
  });

  it("COM-3: レスポンスと動画要素に定義済みスキーマ以外のフィールドを含めない", async () => {
    mockNicoFetch([nvapiRoute("300002", 1, [nvapiItem("sm1", "t")])]);

    const res = await app.request("/api/user/videos?userId=300002", {}, env);
    const body = await expectJson(res);
    expect(Object.keys(body).sort()).toEqual(
      [
        "userId",
        "page",
        "videos",
        "videosCount",
        "hasNext",
        "feedUpdated",
        "cachedAt",
        "fromCache",
      ].sort(),
    );
    const video = (body.videos as Record<string, unknown>[])[0]!;
    expect(Object.keys(video).sort()).toEqual(
      ["videoId", "title", "thumbnail", "published", "link"].sort(),
    );
  });

  it("UV-1: nvapiへ正しいクエリパラメータ（pageSize=30, sortKey, page）でアクセスする", async () => {
    const recorded = mockNicoFetch([nvapiRoute("300003", 0, [])]);

    await app.request("/api/user/videos?userId=300003&page=3", {}, env);
    const call = recorded.find((r) => r.url.origin === NVAPI_ORIGIN);
    expect(call).toBeDefined();
    expect(call!.url.searchParams.get("pageSize")).toBe("30");
    expect(call!.url.searchParams.get("page")).toBe("3");
    expect(call!.url.searchParams.get("sortKey")).toBe("registeredAt");
    expect(call!.url.searchParams.get("sortOrder")).toBe("desc");
  });

  it("UV-2: hasNextを page×30 < totalCount で算出する", async () => {
    // totalCount=61: page1,2はhasNext=true、page3はfalse
    const items = Array.from({ length: 30 }, (_, i) => nvapiItem(`sm${i}`, `v${i}`));
    mockNicoFetch([nvapiRoute("300004", 61, items)]);

    const page1 = await expectJson(
      await app.request("/api/user/videos?userId=300004&page=1", {}, env),
    );
    expect(page1.hasNext).toBe(true);

    const page3 = await expectJson(
      await app.request("/api/user/videos?userId=300004&page=3", {}, env),
    );
    expect(page3.hasNext).toBe(false);
  });

  it("UV-3: userId未指定は400とテキストを返す", async () => {
    mockNicoFetch([]);
    const res = await app.request("/api/user/videos", {}, env);
    expect(res.status).toBe(400);
    expect(await res.text()).toBe("Missing required parameter: userId");
  });

  it("UV-4: userIdが数字以外を含む場合は400とテキストを返す", async () => {
    mockNicoFetch([]);
    const res = await app.request("/api/user/videos?userId=abc123", {}, env);
    expect(res.status).toBe(400);
    expect(await res.text()).toBe("Invalid parameter: userId must be numeric");
  });

  it("UV-5: pageが1未満または整数でない場合は400とテキストを返す", async () => {
    mockNicoFetch([]);
    for (const bad of ["0", "-1", "abc", "1.5"]) {
      const res = await app.request(`/api/user/videos?userId=300005&page=${bad}`, {}, env);
      expect(res.status, `page=${bad}`).toBe(400);
      expect(await res.text()).toBe("Invalid parameter: page must be >= 1");
    }
  });

  it("UV-6: 存在しないユーザーIDは200と空のvideosを返す", async () => {
    // nvapiは不存在ユーザーにも200 + totalCount:0を返す（フェーズ0実測）
    mockNicoFetch([nvapiRoute("999999999", 0, [])]);

    const res = await app.request("/api/user/videos?userId=999999999", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as Record<string, unknown>;
    expect(body.videos).toEqual([]);
    expect(body.videosCount).toBe(0);
    expect(body.hasNext).toBe(false);
  });

  it("COM-1/COM-2: nvapiへのリクエストにUser-AgentとX-Frontend-Id/Versionを付与する", async () => {
    const recorded = mockNicoFetch([nvapiRoute("300006", 0, [])]);

    await app.request("/api/user/videos?userId=300006", {}, env);
    const call = recorded.find((r) => r.url.origin === NVAPI_ORIGIN);
    expect(call).toBeDefined();
    expect(call!.headers.get("user-agent")).toMatch(/Chrome/);
    expect(call!.headers.get("x-frontend-id")).toBe("6");
    expect(call!.headers.get("x-frontend-version")).toBe("0");
  });

  it("COM-4: nvapiがエラーを返す場合は500とテキストを返す", async () => {
    mockNicoFetch([
      {
        origin: NVAPI_ORIGIN,
        handler: () => new Response("oops", { status: 500 }),
      },
    ]);

    const res = await app.request("/api/user/videos?userId=300007", {}, env);
    expect(res.status).toBe(500);
    expect((await res.text()).length).toBeGreaterThan(0);
  });
});
