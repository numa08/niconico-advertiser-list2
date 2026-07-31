// 日時表示の整形（仕様: FRONTEND_REQUIREMENTS.md FE-037, FE-077b, FE-078）
// 整形に失敗した場合は元の文字列をそのまま返す（FE-078）

function pad2(value: number): string {
  return String(value).padStart(2, "0");
}

/** ISO8601をブラウザのローカルタイムゾーンで「{年}年{月}月{日}日」に整形する（FE-037） */
export function formatDateYmd(isoString: string): string {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) {
    return isoString;
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
}

/** ISO8601を「{年}年{月}月{日}日 HH:mm:ss」（時分秒ゼロ埋め2桁）に整形する（FE-077b） */
export function formatDateTimeYmdHms(isoString: string): string {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) {
    return isoString;
  }
  const ymd = `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
  const hms = `${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
  return `${ymd} ${hms}`;
}
