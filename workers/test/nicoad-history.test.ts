// 対応仕様: docs/WORKERS_API_SPEC.md
//   AH-1〜AH-8, COM-1, COM-3, COM-4（/api/video/nicoad-history 分）
//   NF-1（一部: 上限ページ数到達時のサブリクエスト数をAH-2で検証）
import { env } from "cloudflare:workers";
import { afterEach, describe, expect, it, vi } from "vitest";
import { app } from "../src/app";
import {
  KOKEN_ORIGIN,
  KOKEN_PAGE_SIZE,
  expectJson,
  kokenRoute,
  mockNicoFetch,
} from "./helpers";

afterEach(() => {
  vi.restoreAllMocks();
});

const MAX_PAGES_PER_REQUEST = 40;

describe("GET /api/video/nicoad-history", () => {
  it("AH-1: 複数ページをカーソル方式で取得し1リストに結合して返す", async () => {
    // 2.5ページ分 = 250件
    mockNicoFetch([kokenRoute("sm200001", 250)]);

    const res = await app.request("/api/video/nicoad-history?videoId=sm200001", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      histories: Record<string, unknown>[];
      nextOffsetId?: number;
    };
    expect(body.histories.length).toBe(250);
    // フィールド対応: supporterName→advertiserName, id→nicoadId, point→adPoint, supporterId→userId
    const first = body.histories[0]!;
    expect(first.advertiserName).toBe("広告主10000000");
    expect(first.nicoadId).toBe(10000000);
    expect(first.adPoint).toBe(500);
    expect(first.contribution).toBe(500);
    expect(first.startedAt).toBe(1700000000);
    expect(first.endedAt).toBe(1700604800);
    expect(first.userId).toBe(1000);
    // 匿名広告主は userId が null
    expect(body.histories[1]!.userId).toBeNull();
    // messageはkoken APIに対応フィールドがないため常にnull
    expect(first.message).toBeNull();
    // 終端まで取得できたので継続カーソルは含めない
    expect(body.nextOffsetId).toBeUndefined();
  });

  it("COM-3: レスポンスと履歴要素に定義済みスキーマ以外のフィールドを含めない", async () => {
    mockNicoFetch([kokenRoute("sm200002", 3)]);

    const res = await app.request("/api/video/nicoad-history?videoId=sm200002", {}, env);
    const body = await expectJson(res);
    // 終端到達時: nextOffsetIdなし
    expect(Object.keys(body).sort()).toEqual(["cachedAt", "fromCache", "histories"].sort());
    const item = (body.histories as Record<string, unknown>[])[0]!;
    expect(Object.keys(item).sort()).toEqual(
      [
        "advertiserName",
        "nicoadId",
        "adPoint",
        "contribution",
        "startedAt",
        "endedAt",
        "userId",
        "message",
      ].sort(),
    );
  });

  it("AH-2/AH-3/NF-1: 上限40ページで打ち切り、継続カーソルを返す（サブリクエスト上限対策）", async () => {
    // 41ページ分（4,100件）用意し、40ページで打ち切られることを確認する
    const recorded = mockNicoFetch([kokenRoute("sm200003", 4100)]);

    const res = await app.request("/api/video/nicoad-history?videoId=sm200003", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      histories: unknown[];
      nextOffsetId?: number;
    };
    expect(body.histories.length).toBe(MAX_PAGES_PER_REQUEST * KOKEN_PAGE_SIZE);
    // 4000件目のid = 10000000 - 4000 + 1
    expect(body.nextOffsetId).toBe(10_000_000 - 4000 + 1);
    // kokenへのfetchはちょうど40回（NF-1: KV操作と合わせても50回未満）
    const kokenCalls = recorded.filter((r) => r.url.origin === KOKEN_ORIGIN);
    expect(kokenCalls.length).toBe(MAX_PAGES_PER_REQUEST);
  });

  it("AH-4: offsetIdパラメータを指定するとそのカーソル位置から取得を開始する", async () => {
    const cursorId = 10_000_000 - 4000 + 1;
    const recorded = mockNicoFetch([kokenRoute("sm200004", 4100)]);

    const res = await app.request(
      `/api/video/nicoad-history?videoId=sm200004&offsetId=${cursorId}`,
      {},
      env,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      histories: unknown[];
      nextOffsetId?: number;
    };
    // 残り100件をすべて取得し終端に達する
    expect(body.histories.length).toBe(100);
    expect(body.nextOffsetId).toBeUndefined();
    // 最初のkoken呼び出しに指定したoffsetIdが渡っている
    const firstKokenCall = recorded.find((r) => r.url.origin === KOKEN_ORIGIN);
    expect(firstKokenCall?.url.searchParams.get("offsetId")).toBe(String(cursorId));
  });

  it("AH-5: ページ間に10ms以上の遅延を挿入する", async () => {
    // 3ページ分 = ページ間の遅延2回 = 最低20ms
    mockNicoFetch([kokenRoute("sm200005", 300)]);

    const started = Date.now();
    const res = await app.request("/api/video/nicoad-history?videoId=sm200005", {}, env);
    const elapsed = Date.now() - started;
    expect(res.status).toBe(200);
    expect(elapsed).toBeGreaterThanOrEqual(20);
  });

  it("AH-6: 存在しない動画IDは200と空のhistoriesを返す", async () => {
    // koken APIは不存在動画にも200 + 空リストを返す（フェーズ0実測）
    mockNicoFetch([kokenRoute("sm200006", 0)]);

    const res = await app.request("/api/video/nicoad-history?videoId=sm200006", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as { histories: unknown[] };
    expect(body.histories).toEqual([]);
  });

  it("AH-7: videoId未指定・空文字列は400を返す", async () => {
    mockNicoFetch([]);
    expect((await app.request("/api/video/nicoad-history", {}, env)).status).toBe(400);
    expect(
      (await app.request("/api/video/nicoad-history?videoId=", {}, env)).status,
    ).toBe(400);
  });

  it("AH-8: videoIdに英数字以外が含まれる場合は400を返し、ニコニコへアクセスしない", async () => {
    const recorded = mockNicoFetch([]);
    const res = await app.request(
      `/api/video/nicoad-history?videoId=${encodeURIComponent("sm9/../evil")}`,
      {},
      env,
    );
    expect(res.status).toBe(400);
    expect(recorded.length).toBe(0);
  });

  it("AH-8: offsetIdが正の整数でない場合は400を返す", async () => {
    mockNicoFetch([]);
    for (const bad of ["abc", "-5", "0", "1.5"]) {
      const res = await app.request(
        `/api/video/nicoad-history?videoId=sm200007&offsetId=${bad}`,
        {},
        env,
      );
      expect(res.status, `offsetId=${bad}`).toBe(400);
    }
  });

  it("COM-1: koken APIへのリクエストにChrome相当のUser-Agentを付与する", async () => {
    const recorded = mockNicoFetch([kokenRoute("sm200008", 10)]);

    await app.request("/api/video/nicoad-history?videoId=sm200008", {}, env);
    const kokenCalls = recorded.filter((r) => r.url.origin === KOKEN_ORIGIN);
    expect(kokenCalls.length).toBeGreaterThan(0);
    for (const r of kokenCalls) {
      expect(r.headers.get("user-agent")).toMatch(/Chrome/);
    }
  });

  it("COM-4: koken APIがエラーを返す場合は500とerrorボディを返す", async () => {
    mockNicoFetch([
      {
        origin: KOKEN_ORIGIN,
        handler: () => new Response("oops", { status: 500 }),
      },
    ]);

    const res = await app.request("/api/video/nicoad-history?videoId=sm200009", {}, env);
    expect(res.status).toBe(500);
    const body = (await res.json()) as Record<string, unknown>;
    expect(typeof body.error).toBe("string");
  });
});
