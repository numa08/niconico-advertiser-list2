// ニコニコへのアクセスに共通するヘッダー定義（仕様: COM-1, COM-2）
// User-Agentがないとbot対策に引っかかる（watchページ503 / nvapi 403。フェーズ0実測）

export const USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

export const WATCH_HEADERS: Record<string, string> = {
  Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
  "Accept-Language": "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7",
  "User-Agent": USER_AGENT,
};

export const KOKEN_HEADERS: Record<string, string> = {
  Accept: "application/json",
  "User-Agent": USER_AGENT,
};

export const NVAPI_HEADERS: Record<string, string> = {
  Accept: "application/json",
  "X-Frontend-Id": "6",
  "X-Frontend-Version": "0",
  "User-Agent": USER_AGENT,
};

/** ニコニコ側がエラーHTTPステータスを返したことを表す（COM-4で500に変換する） */
export class UpstreamHttpError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "UpstreamHttpError";
  }
}

/** 動画が存在しない（watchページ404）ことを表す（VI-8で404に変換する） */
export class VideoNotFoundError extends Error {
  constructor(readonly videoId: string) {
    super(`Video not found: ${videoId}`);
    this.name = "VideoNotFoundError";
  }
}

/** エラーログに付与するリクエスト情報 */
export interface RequestLogInfo {
  method: string;
  path: string;
}

/**
 * 構造化ログ（NF-2, NF-3）。
 * スキーマ: {"level":"error","context":string,"method"?:string,"path"?:string,"message":string,"stack"?:string}
 */
export function logError(context: string, error: unknown, request?: RequestLogInfo): void {
  console.error(
    JSON.stringify({
      level: "error",
      context,
      ...(request ?? {}),
      message: error instanceof Error ? error.message : String(error),
      ...(error instanceof Error && error.stack !== undefined ? { stack: error.stack } : {}),
    }),
  );
}
