// 投稿者アイコンURLの組み立て（仕様: FRONTEND_REQUIREMENTS.md FE-016、付録D-8/E）

const ICON_BASE_URL = "https://secure-dcdn.cdn.nimg.jp/nicoaccount/usericon";

/**
 * 投稿者IDからアイコン画像URLを組み立てる。
 * prefixは floor(userId / 10000)（付録Eの実測62/62件で確認された規則。
 * 現行実装の「先頭2文字」は6桁IDでのみ偶然一致するバグであり踏襲しない）
 */
export function userIconUrl(userId: string): string {
  const prefix = Math.floor(Number(userId) / 10000);
  return `${ICON_BASE_URL}/${prefix}/${userId}.jpg`;
}
