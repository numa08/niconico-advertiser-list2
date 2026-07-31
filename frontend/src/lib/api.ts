// バックエンドAPI呼び出し層（仕様: FRONTEND_REQUIREMENTS.md FE-070a, FE-077c, FE-140〜FE-144）
// API契約はdocs/WORKERS_API_SPEC.mdが正。オリジン相対パス /api/* で呼び出す（FE-140）
// TODO: TDDレッドフェーズのスタブ。テスト(test/api.test.ts)を満たす実装に置き換える

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

/** 動画情報を取得する（FE-141, FE-142, FE-144） */
export async function fetchVideoInfo(
  videoId: string,
  options: FetchOptions = {},
): Promise<ApiResult<VideoInfo>> {
  void videoId;
  void options;
  return { ok: false, status: 0, message: "" };
}

/**
 * 広告履歴を全件取得する。
 * nextOffsetIdが返る間はoffsetIdを付与して繰り返し取得し、historiesを結合する
 * （FE-070a。継続リクエストは最大25回で打ち切り）
 */
export async function fetchNicoadHistory(
  videoId: string,
  options: FetchOptions = {},
): Promise<ApiResult<NicoadHistoryResult>> {
  void videoId;
  void options;
  return { ok: false, status: 0, message: "" };
}

/** 投稿動画一覧を取得する（FE-033a, FE-141, FE-142, FE-143） */
export async function fetchUserVideos(
  userId: string,
  page: number,
  options: FetchOptions = {},
): Promise<ApiResult<UserVideosResult>> {
  void userId;
  void page;
  void options;
  return { ok: false, status: 0, message: "" };
}
