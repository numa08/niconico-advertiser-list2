// 対応仕様: docs/FRONTEND_REQUIREMENTS.md
//   FE-070a（継続カーソルのループ取得）、FE-077c（複数チャンクのキャッシュ情報集約）
//   FE-033a（AbortSignalの伝播）、FE-140〜FE-144（API呼び出し・エラーメッセージ）
import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchNicoadHistory, fetchUserVideos, fetchVideoInfo } from "../src/lib/api";

interface RecordedCall {
  url: URL;
  signal: AbortSignal | null | undefined;
}

/** グローバルfetchをモックし、呼び出しを記録する。ハンドラはURLごとにResponseを返す */
function mockFetch(handler: (url: URL, callIndex: number) => Response | Promise<Response>) {
  const calls: RecordedCall[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      // フロントエンドはオリジン相対パスで呼び出すため、基準URLを与えて解釈する（FE-140）
      const url = new URL(String(input), "http://localhost");
      calls.push({ url, signal: init?.signal });
      return handler(url, calls.length - 1);
    }),
  );
  return calls;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("fetchVideoInfo", () => {
  const videoInfoBody = {
    videoId: "sm9",
    title: "タイトル",
    thumbnail: "https://img.example/t.jpg",
    userId: "4",
    cachedAt: "2026-07-31T00:00:00Z",
    fromCache: false,
  };

  it("FE-140/FE-141: オリジン相対の /api/video/info を videoId パラメータ付きで呼び出す", async () => {
    const calls = mockFetch(() => Response.json(videoInfoBody));

    const result = await fetchVideoInfo("sm9");
    expect(result.ok).toBe(true);
    expect(calls.length).toBe(1);
    expect(calls[0]!.url.pathname).toBe("/api/video/info");
    expect(calls[0]!.url.searchParams.get("videoId")).toBe("sm9");
    expect(calls[0]!.url.searchParams.has("refresh")).toBe(false);
  });

  it("FE-141: refresh指定時は refresh=true を付与する", async () => {
    const calls = mockFetch(() => Response.json(videoInfoBody));

    await fetchVideoInfo("sm9", { refresh: true });
    expect(calls.length).toBe(1);
    expect(calls[0]!.url.searchParams.get("refresh")).toBe("true");
  });

  it("FE-142: 動画情報のフィールドを消費して返す", async () => {
    mockFetch(() => Response.json(videoInfoBody));

    const result = await fetchVideoInfo("sm9");
    if (!result.ok) throw new Error("expected ok");
    expect(result.data.videoId).toBe("sm9");
    expect(result.data.title).toBe("タイトル");
    expect(result.data.thumbnail).toBe("https://img.example/t.jpg");
    expect(result.data.userId).toBe("4");
    expect(result.data.cachedAt).toBe("2026-07-31T00:00:00Z");
    expect(result.data.fromCache).toBe(false);
  });

  it("FE-144: エラーステータス時のメッセージは「動画情報の取得に失敗しました: {statusText}」", async () => {
    mockFetch(() => new Response("err", { status: 500, statusText: "Internal Server Error" }));

    const result = await fetchVideoInfo("sm9");
    if (result.ok) throw new Error("expected error");
    expect(result.status).toBe(500);
    expect(result.message).toBe("動画情報の取得に失敗しました: Internal Server Error");
  });

  it("FE-144: 例外発生時のメッセージは「エラーが発生しました: {例外メッセージ}」", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new Error("network down");
      }),
    );

    const result = await fetchVideoInfo("sm9");
    if (result.ok) throw new Error("expected error");
    expect(result.message).toBe("エラーが発生しました: network down");
  });
});

describe("fetchNicoadHistory", () => {
  function historyChunk(
    names: string[],
    options: { cachedAt: string; fromCache: boolean; nextOffsetId?: number },
  ) {
    return {
      histories: names.map((name, i) => ({
        advertiserName: name,
        nicoadId: i,
        adPoint: 100,
        contribution: 100,
        startedAt: 1700000000,
        endedAt: 1700604800,
        userId: null,
        message: null,
      })),
      cachedAt: options.cachedAt,
      fromCache: options.fromCache,
      ...(options.nextOffsetId !== undefined ? { nextOffsetId: options.nextOffsetId } : {}),
    };
  }

  it("FE-070a: nextOffsetIdが返る間はoffsetIdを付与して繰り返し取得し、historiesを受信順に結合する", async () => {
    const calls = mockFetch((url) => {
      const offsetId = url.searchParams.get("offsetId");
      if (offsetId === null) {
        return Response.json(
          historyChunk(["A", "B"], {
            cachedAt: "2026-07-31T01:00:00Z",
            fromCache: false,
            nextOffsetId: 500,
          }),
        );
      }
      return Response.json(
        historyChunk(["C"], { cachedAt: "2026-07-31T01:00:05Z", fromCache: false }),
      );
    });

    const result = await fetchNicoadHistory("sm9");
    if (!result.ok) throw new Error("expected ok");
    expect(calls.length).toBe(2);
    expect(calls[0]!.url.pathname).toBe("/api/video/nicoad-history");
    expect(calls[0]!.url.searchParams.get("offsetId")).toBeNull();
    expect(calls[1]!.url.searchParams.get("offsetId")).toBe("500");
    expect(result.data.histories.map((h) => h.advertiserName)).toEqual(["A", "B", "C"]);
  });

  it("FE-070a: refresh指定はループ中の全リクエストに引き継ぐ", async () => {
    const calls = mockFetch((url) =>
      url.searchParams.get("offsetId") === null
        ? Response.json(
            historyChunk(["A"], {
              cachedAt: "c1",
              fromCache: false,
              nextOffsetId: 10,
            }),
          )
        : Response.json(historyChunk(["B"], { cachedAt: "c2", fromCache: false })),
    );

    await fetchNicoadHistory("sm9", { refresh: true });
    expect(calls.length).toBe(2);
    for (const call of calls) {
      expect(call.url.searchParams.get("refresh")).toBe("true");
    }
  });

  it("FE-070a: 継続リクエストは最大25回で打ち切る", async () => {
    const calls = mockFetch(() =>
      Response.json(historyChunk(["X"], { cachedAt: "c", fromCache: false, nextOffsetId: 1 })),
    );

    const result = await fetchNicoadHistory("sm9");
    expect(result.ok).toBe(true);
    expect(calls.length).toBe(25);
  });

  it("FE-077c: cachedAtは全チャンク中最古の値を返す", async () => {
    mockFetch((url) =>
      url.searchParams.get("offsetId") === null
        ? Response.json(
            historyChunk(["A"], {
              cachedAt: "2026-07-31T01:00:00Z",
              fromCache: true,
              nextOffsetId: 10,
            }),
          )
        : Response.json(historyChunk(["B"], { cachedAt: "2026-07-30T23:00:00Z", fromCache: true })),
    );

    const result = await fetchNicoadHistory("sm9");
    if (!result.ok) throw new Error("expected ok");
    expect(result.data.cachedAt).toBe("2026-07-30T23:00:00Z");
  });

  it("FE-077c: fromCacheは全チャンクが真の場合のみ真", async () => {
    mockFetch((url) =>
      url.searchParams.get("offsetId") === null
        ? Response.json(historyChunk(["A"], { cachedAt: "c1", fromCache: true, nextOffsetId: 10 }))
        : Response.json(historyChunk(["B"], { cachedAt: "c2", fromCache: false })),
    );

    const result = await fetchNicoadHistory("sm9");
    if (!result.ok) throw new Error("expected ok");
    expect(result.data.fromCache).toBe(false);
  });

  it("FE-144: エラーステータス時のメッセージは「広告履歴の取得に失敗しました: {statusText}」", async () => {
    mockFetch(() => new Response("err", { status: 503, statusText: "Service Unavailable" }));

    const result = await fetchNicoadHistory("sm9");
    if (result.ok) throw new Error("expected error");
    expect(result.status).toBe(503);
    expect(result.message).toBe("広告履歴の取得に失敗しました: Service Unavailable");
  });
});

describe("fetchUserVideos", () => {
  const userVideosBody = {
    userId: "4",
    page: 1,
    videos: [
      {
        videoId: "sm1",
        title: "動画1",
        thumbnail: "https://img.example/1.jpg",
        published: "2024-01-15T12:34:56+09:00",
        link: "https://www.nicovideo.jp/watch/sm1",
      },
    ],
    videosCount: 1,
    hasNext: true,
    feedUpdated: null,
    cachedAt: "2026-07-31T00:00:00Z",
    fromCache: false,
  };

  it("FE-141/FE-142: userId・pageパラメータで呼び出し、videos/videosCount/hasNextを消費する", async () => {
    const calls = mockFetch(() => Response.json(userVideosBody));

    const result = await fetchUserVideos("4", 1);
    if (!result.ok) throw new Error("expected ok");
    expect(calls[0]!.url.pathname).toBe("/api/user/videos");
    expect(calls[0]!.url.searchParams.get("userId")).toBe("4");
    expect(calls[0]!.url.searchParams.get("page")).toBe("1");
    expect(result.data.videos[0]!.videoId).toBe("sm1");
    expect(result.data.videos[0]!.published).toBe("2024-01-15T12:34:56+09:00");
    expect(result.data.videosCount).toBe(1);
    expect(result.data.hasNext).toBe(true);
  });

  it("FE-033a: AbortSignalをfetchへ伝播する", async () => {
    const calls = mockFetch(() => Response.json(userVideosBody));
    const controller = new AbortController();

    await fetchUserVideos("4", 1, { signal: controller.signal });
    expect(calls.length).toBe(1);
    expect(calls[0]!.signal).toBe(controller.signal);
  });

  it("FE-143: 404時のメッセージは「ユーザーが見つかりませんでした」", async () => {
    mockFetch(() => new Response("not found", { status: 404, statusText: "Not Found" }));

    const result = await fetchUserVideos("999", 1);
    if (result.ok) throw new Error("expected error");
    expect(result.status).toBe(404);
    expect(result.message).toBe("ユーザーが見つかりませんでした");
  });

  it("FE-143: その他エラー時のメッセージは「動画の取得に失敗しました: {statusText}」", async () => {
    mockFetch(() => new Response("err", { status: 500, statusText: "Internal Server Error" }));

    const result = await fetchUserVideos("4", 1);
    if (result.ok) throw new Error("expected error");
    expect(result.message).toBe("動画の取得に失敗しました: Internal Server Error");
  });

  it("FE-143: 例外発生時のメッセージは「エラーが発生しました: {例外メッセージ}」", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new Error("offline");
      }),
    );

    const result = await fetchUserVideos("4", 1);
    if (result.ok) throw new Error("expected error");
    expect(result.message).toBe("エラーが発生しました: offline");
  });
});
