import { NVAPI_HEADERS, UpstreamHttpError } from "./http";

/** ユーザー投稿動画1件（現行JVM実装のUserVideoと同一スキーマ） */
export interface UserVideoItem {
  videoId: string;
  title: string;
  thumbnail: string;
  published: string;
  link: string;
}

/** 1ページ分の取得結果 */
export interface UserVideosPayload {
  userId: string;
  page: number;
  videos: UserVideoItem[];
  videosCount: number;
  hasNext: boolean;
  feedUpdated: null;
}

interface NvapiVideosResponse {
  meta: { status: number };
  data: {
    totalCount: number;
    items: {
      essential: {
        id: string;
        title: string;
        registeredAt: string;
        thumbnail: { url: string };
      };
    }[];
  };
}

export const NVAPI_PAGE_SIZE = 30;

/**
 * nvapiからユーザー投稿動画一覧を取得する（仕様: UV-1, UV-2, UV-6）。
 * Atomフィードは廃止されたためnvapiを使用する（フェーズ0実測）
 */
export async function fetchUserVideos(userId: string, page: number): Promise<UserVideosPayload> {
  const url =
    `https://nvapi.nicovideo.jp/v3/users/${userId}/videos` +
    `?sortKey=registeredAt&sortOrder=desc&pageSize=${NVAPI_PAGE_SIZE}&page=${page}`;
  const res = await fetch(url, { headers: NVAPI_HEADERS });
  if (!res.ok) {
    await res.body?.cancel();
    throw new UpstreamHttpError(res.status, `Failed to fetch user videos: HTTP ${res.status}`);
  }
  const body = (await res.json()) as NvapiVideosResponse;

  const videos = body.data.items.map((item) => ({
    videoId: item.essential.id,
    title: item.essential.title,
    thumbnail: item.essential.thumbnail.url,
    published: item.essential.registeredAt,
    link: `https://www.nicovideo.jp/watch/${item.essential.id}`,
  }));

  return {
    userId,
    page,
    videos,
    videosCount: videos.length,
    // totalCountから算出（現行の次ページ先読み方式は不要。仕様: UV-2）
    hasNext: page * NVAPI_PAGE_SIZE < body.data.totalCount,
    // Atomフィード廃止に伴い対応する値が存在しない（仕様: UV-1）
    feedUpdated: null,
  };
}
