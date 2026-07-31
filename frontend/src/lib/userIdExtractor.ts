// 投稿者ID抽出（仕様: FRONTEND_REQUIREMENTS.md FE-130〜FE-134）
// 現行KotlinのUserIdExtractorを移植。既存ユニットテストのケース表が受け入れ基準

import { isNiconicoDomain, parseSimpleUrl, stripQueryAndFragment } from "./nicoUrl";

const USER_PATH_PREFIX = "/user/";

/** 1文字以上の数字のみを有効な投稿者IDとする（FE-131） */
function isValidUserId(candidate: string): boolean {
  return /^\d+$/.test(candidate);
}

/** URLまたは投稿者IDの入力から投稿者IDを抽出する。抽出できない場合はnull */
export function extractUserId(input: string): string | null {
  const trimmed = input.trim();
  if (trimmed === "") {
    return null;
  }
  if (isValidUserId(trimmed)) {
    return trimmed;
  }

  const parsed = parseSimpleUrl(trimmed);
  if (parsed === null || !isNiconicoDomain(parsed.host)) {
    return null;
  }
  if (!parsed.path.startsWith(USER_PATH_PREFIX)) {
    return null;
  }
  // `/user/{id}/video` のような後続セグメントは無視してID部分のみを取り出す（FE-133）
  const firstSegment = parsed.path.slice(USER_PATH_PREFIX.length).split("/", 1)[0] ?? "";
  const candidate = stripQueryAndFragment(firstSegment);
  return isValidUserId(candidate) ? candidate : null;
}
