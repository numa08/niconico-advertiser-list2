import type { ErrorHandler } from "hono";
import { Hono } from "hono";
import {
  NICOAD_HISTORY_TTL_SECONDS,
  USER_VIDEOS_TTL_SECONDS,
  VIDEO_INFO_TTL_SECONDS,
  withCache,
} from "./cache";
import { logError, VideoNotFoundError } from "./nico/http";
import { fetchNicoadHistoryChunk } from "./nico/nicoad";
import { fetchUserVideos } from "./nico/nvapi";
import { fetchVideoInfo } from "./nico/watchPage";

/**
 * APIルーター本体
 *
 * 静的アセット（Kobwebエクスポート成果物）は wrangler.jsonc の assets 設定で
 * 配信されるため、この Worker は /api/* のみを処理する（仕様: NF-4）。
 * 仕様は docs/WORKERS_API_SPEC.md を参照。
 */
export const app = new Hono<{ Bindings: Env }>();

/** videoIdはURLパスへ連結するため英数字のみ許可する（仕様: VI-7, AH-8） */
const VIDEO_ID_PATTERN = /^[a-zA-Z0-9]+$/;
/** offsetId・userId・pageの数値パラメータ形式 */
const POSITIVE_INT_PATTERN = /^[1-9]\d*$/;
const DIGITS_PATTERN = /^\d+$/;

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Unknown error";
}

/**
 * ハンドラ内のtry/catchを通過しなかった予期しない例外の最終防衛線（仕様: NF-3）。
 * エラーボディの形式はエンドポイント系統に合わせる（/api/user/*はテキスト、他はJSON）
 */
export const handleUnexpectedError: ErrorHandler<{ Bindings: Env }> = (error, c) => {
  logError("app.onError", error, { method: c.req.method, path: c.req.path });
  if (c.req.path.startsWith("/api/user/")) {
    return c.text(`Internal server error: ${errorMessage(error)}`, 500);
  }
  return c.json({ error: errorMessage(error) }, 500);
};

app.onError(handleUnexpectedError);

// /api/* 以外は Static Assets へ委譲する（仕様: NF-4）。
// ナビゲーションリクエストはプラットフォームがWorker前段でSPAフォールバックを適用するが、
// Sec-Fetch-Modeを送らないクライアントはWorkerに到達するため、こちらでも委譲する
app.notFound((c) => {
  if (c.req.path.startsWith("/api/")) {
    return c.text("404 Not Found", 404);
  }
  return c.env.ASSETS.fetch(c.req.raw);
});

app.get("/api/health", (c) => c.json({ status: "ok" }));

// 仕様: VI-1〜VI-8
app.get("/api/video/info", async (c) => {
  const videoId = c.req.query("videoId");
  if (videoId === undefined || videoId === "" || !VIDEO_ID_PATTERN.test(videoId)) {
    return c.body(null, 400);
  }
  const refresh = c.req.query("refresh") === "true";

  try {
    const result = await withCache(
      c.env.NICO_CACHE,
      `video-info:${videoId}`,
      VIDEO_INFO_TTL_SECONDS,
      refresh,
      () => fetchVideoInfo(videoId),
    );
    return c.json({
      ...result.payload,
      cachedAt: result.cachedAt,
      fromCache: result.fromCache,
    });
  } catch (error) {
    if (error instanceof VideoNotFoundError) {
      return c.json({ error: error.message }, 404);
    }
    logError("api.video.info", error, { method: c.req.method, path: c.req.path });
    return c.json({ error: errorMessage(error) }, 500);
  }
});

// 仕様: AH-1〜AH-8
app.get("/api/video/nicoad-history", async (c) => {
  const videoId = c.req.query("videoId");
  if (videoId === undefined || videoId === "" || !VIDEO_ID_PATTERN.test(videoId)) {
    return c.body(null, 400);
  }
  const offsetIdParam = c.req.query("offsetId");
  if (offsetIdParam !== undefined && !POSITIVE_INT_PATTERN.test(offsetIdParam)) {
    return c.body(null, 400);
  }
  const offsetId = offsetIdParam === undefined ? undefined : Number(offsetIdParam);
  const refresh = c.req.query("refresh") === "true";

  try {
    const result = await withCache(
      c.env.NICO_CACHE,
      `nicoad-history:${videoId}:${offsetId ?? "start"}`,
      NICOAD_HISTORY_TTL_SECONDS,
      refresh,
      () => fetchNicoadHistoryChunk(videoId, offsetId),
    );
    return c.json({
      histories: result.payload.histories,
      cachedAt: result.cachedAt,
      fromCache: result.fromCache,
      // 継続カーソルは未完了時のみ含める（AH-3。フロントの厳格パースを壊さないため）
      ...(result.payload.nextOffsetId !== undefined
        ? { nextOffsetId: result.payload.nextOffsetId }
        : {}),
    });
  } catch (error) {
    logError("api.video.nicoad-history", error, { method: c.req.method, path: c.req.path });
    return c.json({ error: errorMessage(error) }, 500);
  }
});

// 仕様: UV-1〜UV-6（エラーボディは現行互換のプレーンテキスト）
app.get("/api/user/videos", async (c) => {
  const userId = c.req.query("userId");
  if (userId === undefined) {
    return c.text("Missing required parameter: userId", 400);
  }
  const pageParam = c.req.query("page");
  if (pageParam !== undefined && !POSITIVE_INT_PATTERN.test(pageParam)) {
    return c.text("Invalid parameter: page must be >= 1", 400);
  }
  if (!DIGITS_PATTERN.test(userId)) {
    return c.text("Invalid parameter: userId must be numeric", 400);
  }
  const page = pageParam === undefined ? 1 : Number(pageParam);
  const refresh = c.req.query("refresh") === "true";

  try {
    const result = await withCache(
      c.env.NICO_CACHE,
      `user-videos:${userId}:${page}`,
      USER_VIDEOS_TTL_SECONDS,
      refresh,
      () => fetchUserVideos(userId, page),
    );
    return c.json({
      ...result.payload,
      cachedAt: result.cachedAt,
      fromCache: result.fromCache,
    });
  } catch (error) {
    logError("api.user.videos", error, { method: c.req.method, path: c.req.path });
    return c.text(`Internal server error: ${errorMessage(error)}`, 500);
  }
});
