import { logError } from "./nico/http";

// キャッシュTTL（仕様: CA-1）
export const VIDEO_INFO_TTL_SECONDS = 3600;
export const NICOAD_HISTORY_TTL_SECONDS = 3600;
export const USER_VIDEOS_TTL_SECONDS = 1800;

/** KVに保存する値の形式（仕様: 用語「キャッシュエンベロープ」） */
export interface CacheEnvelope<T> {
  payload: T;
  cachedAt: string;
}

/**
 * キャッシュ読み取り。KV障害時はnullを返しキャッシュミスとして扱う（CA-7）
 */
export async function readCache<T>(
  kv: KVNamespace,
  key: string,
): Promise<CacheEnvelope<T> | null> {
  try {
    return await kv.get<CacheEnvelope<T>>(key, "json");
  } catch (error) {
    logError(`cache.read:${key}`, error);
    return null;
  }
}

/**
 * キャッシュ書き込み。取得成功時のみ呼ぶこと（CA-6）。
 * KV障害時はログのみ残し、レスポンス処理は継続する（CA-7）
 */
export async function writeCache<T>(
  kv: KVNamespace,
  key: string,
  envelope: CacheEnvelope<T>,
  ttlSeconds: number,
): Promise<void> {
  try {
    await kv.put(key, JSON.stringify(envelope), { expirationTtl: ttlSeconds });
  } catch (error) {
    logError(`cache.write:${key}`, error);
  }
}

export interface CachedResult<T> {
  payload: T;
  cachedAt: string;
  fromCache: boolean;
}

/**
 * キャッシュ読み取り→ミス時ロード→書き込み、の共通フロー（CA-2〜CA-4）。
 * fromCacheは実際にキャッシュから応答した場合のみtrue（CA-4）
 */
export async function withCache<T>(
  kv: KVNamespace,
  key: string,
  ttlSeconds: number,
  refresh: boolean,
  loader: () => Promise<T>,
): Promise<CachedResult<T>> {
  if (!refresh) {
    const hit = await readCache<T>(kv, key);
    if (hit !== null) {
      return { payload: hit.payload, cachedAt: hit.cachedAt, fromCache: true };
    }
  }
  const payload = await loader();
  const envelope: CacheEnvelope<T> = { payload, cachedAt: new Date().toISOString() };
  await writeCache(kv, key, envelope, ttlSeconds);
  return { payload, cachedAt: envelope.cachedAt, fromCache: false };
}
