// 広告主リストの整形アルゴリズム（仕様: FRONTEND_REQUIREMENTS.md FE-100〜FE-110）
// TODO: TDDレッドフェーズのスタブ。テスト(test/formatAdvertiserList.test.ts)を満たす実装に置き換える

/** リスト表示形式（FE-093）。仕様の表示ラベルをそのまま値として使う */
export const DISPLAY_FORMATS = [
  "すべて表示",
  "すべて表示（逆順）",
  "同じ名前をまとめる",
  "同じ名前をまとめる（逆順）",
] as const;

export type DisplayFormat = (typeof DISPLAY_FORMATS)[number];

/** 敬称の選択肢（FE-091） */
export const HONORIFIC_OPTIONS = ["様", "さん", "氏", "ちゃん", "くん", "カスタム"] as const;

export type HonorificOption = (typeof HONORIFIC_OPTIONS)[number];

export const DEFAULT_CHARS_PER_LINE = 50;

/** 「1行の文字数」入力の解釈（FE-109） */
export function parseCharsPerLine(input: string): number {
  void input;
  return 0;
}

/** 敬称サフィックスの解決（FE-100） */
export function resolveHonorific(selection: HonorificOption, customValue: string): string {
  void selection;
  void customValue;
  return "";
}

/** 広告主名リストを表示形式・敬称・1行文字数で整形する（FE-101〜FE-108, FE-110） */
export function formatAdvertiserList(
  advertiserNames: readonly string[],
  displayFormat: DisplayFormat,
  honorificSuffix: string,
  charsPerLine: number,
): string {
  void advertiserNames;
  void displayFormat;
  void honorificSuffix;
  void charsPerLine;
  return "";
}
