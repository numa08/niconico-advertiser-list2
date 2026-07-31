import { expect, vi } from "vitest";

/**
 * ステータスコードを検証してからJSONボディを返す。
 * 未実装・異常時にJSONパースエラーではなくステータス不一致として失敗させる
 */
export async function expectJson(
  res: Response,
  expectedStatus = 200,
): Promise<Record<string, unknown>> {
  expect(res.status).toBe(expectedStatus);
  expect(res.headers.get("content-type")).toMatch(/application\/json/);
  return (await res.json()) as Record<string, unknown>;
}

/** モックが記録した外部リクエスト */
export interface RecordedRequest {
  url: URL;
  headers: Headers;
  method: string;
}

export interface MockRoute {
  /** 例: "https://www.nicovideo.jp" */
  origin: string;
  /** パスの一致条件。省略時はorigin一致のみで適用 */
  path?: string | RegExp;
  handler: (url: URL, request: Request) => Response | Promise<Response>;
}

/**
 * グローバルfetchをルーティング式のモックに差し替える。
 * vitest-pool-workers(vitest 4)ではfetchMockが廃止されたため、
 * 公式exampleに倣い vi.spyOn(globalThis, "fetch") を使う。
 * 各テストの後始末は vi.restoreAllMocks() で行うこと。
 */
export function mockNicoFetch(routes: MockRoute[]): RecordedRequest[] {
  const recorded: RecordedRequest[] = [];
  vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
    const request = new Request(input, init);
    const url = new URL(request.url);
    recorded.push({ url, headers: request.headers, method: request.method });
    for (const route of routes) {
      if (url.origin !== route.origin) continue;
      if (route.path instanceof RegExp && !route.path.test(url.pathname)) continue;
      if (typeof route.path === "string" && url.pathname !== route.path) continue;
      return route.handler(url, request);
    }
    throw new Error(`No mock route for ${request.method} ${request.url}`);
  });
  return recorded;
}

export const WATCH_ORIGIN = "https://www.nicovideo.jp";
export const KOKEN_ORIGIN = "https://api.koken.nicovideo.jp";
export const NVAPI_ORIGIN = "https://nvapi.nicovideo.jp";

/** watchページのHTMLフィクスチャ。実ページ同様、JSON-LD内のURLは `\/` エスケープされている */
export function watchPageHtml(options: {
  ogTitle?: string;
  ogImage?: string;
  titleTag?: string;
  jsonLdAuthorUrl?: string;
}): string {
  const metaTags = [
    options.ogTitle !== undefined
      ? `<meta property="og:title" content="${options.ogTitle}">`
      : "",
    options.ogImage !== undefined
      ? `<meta property="og:image" content="${options.ogImage}">`
      : "",
  ].join("\n");
  const jsonLd =
    options.jsonLdAuthorUrl !== undefined
      ? `<script type="application/ld+json" data-server="1">{"@context":"http:\\/\\/schema.org\\/","@type":"VideoObject","name":"dummy","author":{"@type":"Person","url":"${options.jsonLdAuthorUrl.replaceAll("/", "\\/")}"}}</script>`
      : "";
  return `<!DOCTYPE html>
<html lang="ja">
<head>
<title>${options.titleTag ?? ""}</title>
${metaTags}
<script type="application/ld+json">{"@type":"BreadcrumbList","itemListElement":[]}</script>
${jsonLd}
</head>
<body><div id="root"></div></body>
</html>`;
}

interface KokenHistory {
  id: number;
  supporterId: number | null;
  supporterName: string;
  supporterThumbnailUrl: string;
  point: number;
  contribution: number;
  startedAt: number;
  endedAt: number;
  isCurrentlyAdvertising: boolean;
}

export const KOKEN_PAGE_SIZE = 100;

/** koken広告APIの1ページ分の履歴を生成する。idは firstId から降順 */
export function kokenHistories(firstId: number, count: number): KokenHistory[] {
  return Array.from({ length: count }, (_, i) => ({
    id: firstId - i,
    supporterId: i % 2 === 0 ? 1000 + i : null,
    supporterName: `広告主${firstId - i}`,
    supporterThumbnailUrl: "https://example.com/icon.png",
    point: 500,
    contribution: 500,
    startedAt: 1700000000,
    endedAt: 1700604800,
    isCurrentlyAdvertising: false,
  }));
}

/**
 * koken広告APIのモックハンドラを生成する。
 * totalItems件の履歴をカーソル方式（offsetId）で配信する。
 * idは 10_000_000 から連番降順。
 */
export function kokenRoute(videoId: string, totalItems: number): MockRoute {
  const FIRST_ID = 10_000_000;
  return {
    origin: KOKEN_ORIGIN,
    path: `/v1/userperspective/contents/nicoad/video/${videoId}/histories`,
    handler: (url) => {
      const offsetIdParam = url.searchParams.get("offsetId");
      // offsetId は「前ページ最後の履歴id」。次ページはその次のidから始まる
      const nextFirstId = offsetIdParam === null ? FIRST_ID : Number(offsetIdParam) - 1;
      const remainingAfterCursor = nextFirstId - (FIRST_ID - totalItems);
      const count = Math.max(0, Math.min(KOKEN_PAGE_SIZE, remainingAfterCursor));
      const histories = kokenHistories(nextFirstId, count);
      const nextCount = Math.max(0, remainingAfterCursor - count);
      return Response.json({
        meta: { status: 200 },
        data: { nextCount, totalPoint: totalItems * 500, histories },
      });
    },
  };
}

interface NvapiItem {
  essential: {
    id: string;
    title: string;
    registeredAt: string;
    thumbnail: { url: string; middleUrl: string | null; largeUrl: string };
  };
}

/** nvapiユーザー動画APIのレスポンスを生成する */
export function nvapiResponse(totalCount: number, items: NvapiItem[]): unknown {
  return { meta: { status: 200 }, data: { totalCount, items } };
}

export function nvapiItem(id: string, title: string): NvapiItem {
  return {
    essential: {
      id,
      title,
      registeredAt: "2024-01-15T12:34:56+09:00",
      thumbnail: {
        url: `https://nicovideo.cdn.nimg.jp/thumbnails/${id}/default.jpg`,
        middleUrl: null,
        largeUrl: `https://nicovideo.cdn.nimg.jp/thumbnails/${id}/large.jpg`,
      },
    },
  };
}
