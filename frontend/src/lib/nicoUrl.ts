// ニコニコ動画URLの簡易パース（仕様: FRONTEND_REQUIREMENTS.md FE-123, FE-133）
// 現行Kotlin実装（VideoIdExtractor/UserIdExtractorの簡易URLパーサー）を移植したもの。
// スキーム名は検証せず `://` の存在のみを見る（FE-123の注記どおり現行挙動を踏襲）

export interface ParsedNicoUrl {
  host: string;
  path: string;
}

/** `://` 区切りの簡易URLパース。URLでなければnull */
export function parseSimpleUrl(input: string): ParsedNicoUrl | null {
  const schemeEnd = input.indexOf("://");
  if (schemeEnd === -1) {
    return null;
  }
  const remaining = input.slice(schemeEnd + "://".length);
  const pathStart = remaining.indexOf("/");
  if (pathStart === -1) {
    return { host: remaining, path: "/" };
  }
  return { host: remaining.slice(0, pathStart), path: remaining.slice(pathStart) };
}

/** ホストが nicovideo.jp またはそのサブドメインか（大文字小文字は区別しない） */
export function isNiconicoDomain(host: string): boolean {
  const lowerHost = host.toLowerCase();
  return lowerHost === "nicovideo.jp" || lowerHost.endsWith(".nicovideo.jp");
}

/** パスセグメントから `?` / `#` 以降を除去する */
export function stripQueryAndFragment(segment: string): string {
  const beforeQuery = segment.split("?", 1)[0] ?? segment;
  return beforeQuery.split("#", 1)[0] ?? beforeQuery;
}
