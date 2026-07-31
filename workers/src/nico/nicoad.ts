import { KOKEN_HEADERS, UpstreamHttpError } from "./http";

/** 広告履歴1件（APIレスポンス形式。現行JVM実装のNicoadHistoryと同一スキーマ） */
export interface NicoadHistoryItem {
  advertiserName: string;
  nicoadId: number;
  adPoint: number;
  contribution: number;
  startedAt: number;
  endedAt: number;
  userId: number | null;
  message: string | null;
}

/** 1リクエスト分の集約結果。nextOffsetIdありは継続カーソル（仕様: AH-3） */
export interface NicoadHistoryChunk {
  histories: NicoadHistoryItem[];
  nextOffsetId?: number;
}

interface KokenResponse {
  meta: { status: number };
  data: {
    nextCount: number;
    totalPoint: number;
    histories: {
      id: number;
      supporterId: number | null;
      supporterName: string;
      point: number;
      contribution: number;
      startedAt: number;
      endedAt: number;
    }[];
  };
}

const KOKEN_PAGE_LIMIT = 100;
// 無料プランのサブリクエスト上限50回/リクエストに対し、KV読み書きを含めて余裕を残す（仕様: AH-2, NF-1）
export const MAX_PAGES_PER_REQUEST = 40;
const MIN_PAGE_DELAY_MS = 10;
const MAX_PAGE_DELAY_MS = 100;

function kokenUrl(videoId: string, offsetId: number | undefined): string {
  const base = `https://api.koken.nicovideo.jp/v1/userperspective/contents/nicoad/video/${videoId}/histories?limit=${KOKEN_PAGE_LIMIT}`;
  return offsetId === undefined ? base : `${base}&offsetId=${offsetId}`;
}

/** 連続アクセスによる負荷を避けるためのページ間ランダム遅延（仕様: AH-5） */
function randomPageDelay(): Promise<void> {
  const ms =
    MIN_PAGE_DELAY_MS + Math.floor(Math.random() * (MAX_PAGE_DELAY_MS - MIN_PAGE_DELAY_MS + 1));
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * koken広告APIをカーソル方式で取得し、最大40ページを1チャンクに集約する
 * （仕様: AH-1〜AH-5。継続カーソル方式=案b）
 */
export async function fetchNicoadHistoryChunk(
  videoId: string,
  initialOffsetId: number | undefined,
): Promise<NicoadHistoryChunk> {
  const histories: NicoadHistoryItem[] = [];
  let cursor = initialOffsetId;
  let nextOffsetId: number | undefined;

  for (let page = 0; page < MAX_PAGES_PER_REQUEST; page++) {
    if (page > 0) {
      await randomPageDelay();
    }
    const res = await fetch(kokenUrl(videoId, cursor), { headers: KOKEN_HEADERS });
    if (!res.ok) {
      await res.body?.cancel();
      throw new UpstreamHttpError(res.status, `Failed to fetch nicoad histories: HTTP ${res.status}`);
    }
    const body = (await res.json()) as KokenResponse;
    const items = body.data.histories;
    for (const item of items) {
      histories.push({
        advertiserName: item.supporterName,
        nicoadId: item.id,
        adPoint: item.point,
        contribution: item.contribution,
        startedAt: item.startedAt,
        endedAt: item.endedAt,
        userId: item.supporterId ?? null,
        message: null,
      });
    }
    // 存在しない動画もkokenは200 + 空リストを返すため、そのまま空チャンクになる（AH-6）
    if (body.data.nextCount <= 0 || items.length === 0) {
      nextOffsetId = undefined;
      break;
    }
    cursor = items[items.length - 1]!.id;
    nextOffsetId = cursor;
  }

  return nextOffsetId === undefined ? { histories } : { histories, nextOffsetId };
}
