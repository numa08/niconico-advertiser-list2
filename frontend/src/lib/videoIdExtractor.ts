// 動画ID抽出（仕様: FRONTEND_REQUIREMENTS.md FE-120〜FE-124）
// 現行KotlinのVideoIdExtractorを移植。既存ユニットテストのケース表が受け入れ基準

import { isNiconicoDomain, parseSimpleUrl, stripQueryAndFragment } from "./nicoUrl";

const WATCH_PATH_PREFIX = "/watch/";

/** `sm` + 1文字以上の数字のみを有効な動画IDとする（FE-121。大文字SMは不可） */
function isValidVideoId(candidate: string): boolean {
  return /^sm\d+$/.test(candidate);
}

/** URLまたは動画IDの入力から動画IDを抽出する。抽出できない場合はnull */
export function extractVideoId(input: string): string | null {
  const trimmed = input.trim();
  if (trimmed === "") {
    return null;
  }
  if (isValidVideoId(trimmed)) {
    return trimmed;
  }

  const parsed = parseSimpleUrl(trimmed);
  if (parsed === null || !isNiconicoDomain(parsed.host)) {
    return null;
  }
  if (!parsed.path.startsWith(WATCH_PATH_PREFIX)) {
    return null;
  }
  const candidate = stripQueryAndFragment(parsed.path.slice(WATCH_PATH_PREFIX.length));
  return isValidVideoId(candidate) ? candidate : null;
}
