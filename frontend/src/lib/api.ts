// バックエンドAPI呼び出し層（仕様: FRONTEND_REQUIREMENTS.md FE-070a, FE-077c, FE-140〜FE-144）
// API契約はdocs/WORKERS_API_SPEC.mdが正。オリジン相対パス /api/* で呼び出す（FE-140）

export interface VideoInfo {
  videoId: string;
  title: string;
  thumbnail: string;
  userId: string;
  cachedAt: string | null;
  fromCache: boolean;
}

export interface NicoadHistoryItem {
  advertiserName: string;
  contribution: number;
}

/** 継続カーソル（FE-070a）を辿って結合した広告履歴の最終結果 */
export interface NicoadHistoryResult {
  histories: NicoadHistoryItem[];
  /** 全チャンク中最古のcachedAt（FE-077c） */
  cachedAt: string | null;
  /** 全チャンクがキャッシュ応答だった場合のみtrue（FE-077c） */
  fromCache: boolean;
}

export interface UserVideo {
  videoId: string;
  title: string;
  thumbnail: string;
  published: string;
}

export interface UserVideosResult {
  videos: UserVideo[];
  videosCount: number;
  hasNext: boolean;
}

export type ApiResult<T> = { ok: true; data: T } | { ok: false; status: number; message: string };

export interface FetchOptions {
  refresh?: boolean;
  signal?: AbortSignal;
}

/** 継続リクエストの上限。サーバーは1チャンク最大4,000件を返すため約10万件まで（FE-070a） */
const MAX_HISTORY_CHUNK_REQUESTS = 25;

const UNEXPECTED_ERROR_STATUS = 0;

function exceptionResult(error: unknown): { ok: false; status: number; message: string } {
  const message = error instanceof Error ? error.message : String(error);
  return {
    ok: false,
    status: UNEXPECTED_ERROR_STATUS,
    message: `エラーが発生しました: ${message}`,
  };
}

interface VideoInfoResponse {
  videoId: string;
  title: string;
  thumbnail: string;
  userId: string;
  cachedAt?: string | null;
  fromCache?: boolean;
}

/** 動画情報を取得する（FE-141, FE-142, FE-144） */
export async function fetchVideoInfo(
  videoId: string,
  options: FetchOptions = {},
): Promise<ApiResult<VideoInfo>> {
  const params = new URLSearchParams({ videoId });
  if (options.refresh === true) {
    params.set("refresh", "true");
  }
  try {
    const res = await fetch(`/api/video/info?${params.toString()}`, { signal: options.signal });
    if (!res.ok) {
      return {
        ok: false,
        status: res.status,
        message: `動画情報の取得に失敗しました: ${res.statusText}`,
      };
    }
    const body = (await res.json()) as VideoInfoResponse;
    return {
      ok: true,
      data: {
        videoId: body.videoId,
        title: body.title,
        thumbnail: body.thumbnail,
        userId: body.userId,
        cachedAt: body.cachedAt ?? null,
        fromCache: body.fromCache ?? false,
      },
    };
  } catch (error) {
    return exceptionResult(error);
  }
}

interface NicoadHistoryChunkResponse {
  histories: { advertiserName: string; contribution: number }[];
  cachedAt?: string | null;
  fromCache?: boolean;
  nextOffsetId?: number;
}

/** ISO8601文字列として古い方を返す。パースできない場合は辞書順で比較する（FE-077でも使用） */
export function olderCachedAt(a: string | null, b: string): string {
  if (a === null) {
    return b;
  }
  const [aTime, bTime] = [Date.parse(a), Date.parse(b)];
  if (Number.isNaN(aTime) || Number.isNaN(bTime)) {
    return b < a ? b : a;
  }
  return bTime < aTime ? b : a;
}

/**
 * 広告履歴を全件取得する。
 * nextOffsetIdが返る間はoffsetIdを付与して繰り返し取得し、historiesを受信順に結合する
 * （FE-070a。refresh指定は全リクエストへ引き継ぎ、継続リクエストは最大25回で打ち切る）
 */
export async function fetchNicoadHistory(
  videoId: string,
  options: FetchOptions = {},
): Promise<ApiResult<NicoadHistoryResult>> {
  const histories: NicoadHistoryItem[] = [];
  let oldest: string | null = null;
  let allFromCache = true;
  let offsetId: number | undefined;

  try {
    for (let requestCount = 0; requestCount < MAX_HISTORY_CHUNK_REQUESTS; requestCount++) {
      const params = new URLSearchParams({ videoId });
      if (offsetId !== undefined) {
        params.set("offsetId", String(offsetId));
      }
      if (options.refresh === true) {
        params.set("refresh", "true");
      }
      const res = await fetch(`/api/video/nicoad-history?${params.toString()}`, {
        signal: options.signal,
      });
      if (!res.ok) {
        return {
          ok: false,
          status: res.status,
          message: `広告履歴の取得に失敗しました: ${res.statusText}`,
        };
      }
      const body = (await res.json()) as NicoadHistoryChunkResponse;
      for (const item of body.histories) {
        histories.push({ advertiserName: item.advertiserName, contribution: item.contribution });
      }
      if (body.cachedAt !== undefined && body.cachedAt !== null) {
        oldest = olderCachedAt(oldest, body.cachedAt);
      }
      allFromCache = allFromCache && (body.fromCache ?? false);
      if (body.nextOffsetId === undefined) {
        break;
      }
      offsetId = body.nextOffsetId;
    }
    return { ok: true, data: { histories, cachedAt: oldest, fromCache: allFromCache } };
  } catch (error) {
    return exceptionResult(error);
  }
}

interface UserVideosResponse {
  videos: { videoId: string; title: string; thumbnail: string; published: string }[];
  videosCount: number;
  hasNext: boolean;
}

/** 投稿動画一覧を取得する（FE-033a, FE-141, FE-142, FE-143） */
export async function fetchUserVideos(
  userId: string,
  page: number,
  options: FetchOptions = {},
): Promise<ApiResult<UserVideosResult>> {
  const params = new URLSearchParams({ userId, page: String(page) });
  try {
    const res = await fetch(`/api/user/videos?${params.toString()}`, { signal: options.signal });
    if (res.status === 404) {
      return { ok: false, status: 404, message: "ユーザーが見つかりませんでした" };
    }
    if (!res.ok) {
      return {
        ok: false,
        status: res.status,
        message: `動画の取得に失敗しました: ${res.statusText}`,
      };
    }
    const body = (await res.json()) as UserVideosResponse;
    return {
      ok: true,
      data: {
        videos: body.videos.map((video) => ({
          videoId: video.videoId,
          title: video.title,
          thumbnail: video.thumbnail,
          published: video.published,
        })),
        videosCount: body.videosCount,
        hasNext: body.hasNext,
      },
    };
  } catch (error) {
    return exceptionResult(error);
  }
}
