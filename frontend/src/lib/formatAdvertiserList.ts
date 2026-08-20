// 広告主リストの整形アルゴリズム（仕様: FRONTEND_REQUIREMENTS.md FE-100〜FE-110、付録D）

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

/** 広告主名の区切り文字の選択肢（FE-096） */
export const NAME_SEPARATOR_OPTIONS = ["読点（、）", "句点（。）", "カスタム"] as const;

export type NameSeparatorOption = (typeof NAME_SEPARATOR_OPTIONS)[number];

/** 選択肢に対応する実際の区切り文字（FE-100a）。「カスタム」はカスタム入力値を使う */
const NAME_SEPARATOR_CHARS: Record<Exclude<NameSeparatorOption, "カスタム">, string> = {
  "読点（、）": "、",
  "句点（。）": "。",
};

export const DEFAULT_CHARS_PER_LINE = 50;

/** 区切り文字の既定値（FE-096） */
export const DEFAULT_NAME_SEPARATOR_OPTION: NameSeparatorOption = "読点（、）";

/**
 * 「1行の文字数」入力の解釈（FE-109）。
 * 整数として解釈できなければ既定値50、1未満は1にクランプする
 */
export function parseCharsPerLine(input: string): number {
  const trimmed = input.trim();
  if (!/^[+-]?\d+$/.test(trimmed)) {
    return DEFAULT_CHARS_PER_LINE;
  }
  const value = Number(trimmed);
  return value < 1 ? 1 : value;
}

/** 敬称サフィックスの解決（FE-100）。カスタム選択時のみカスタム入力値を使う */
export function resolveHonorific(selection: HonorificOption, customValue: string): string {
  return selection === "カスタム" ? customValue : selection;
}

/** 区切り文字の解決（FE-100a）。カスタム選択時のみカスタム入力値を使う */
export function resolveNameSeparator(selection: NameSeparatorOption, customValue: string): string {
  return selection === "カスタム" ? customValue : NAME_SEPARATOR_CHARS[selection];
}

/** 重複除去。最初の出現を残す（FE-103） */
function dedupeKeepFirst(names: readonly string[]): string[] {
  return [...new Set(names)];
}

function applyDisplayFormat(
  advertiserNames: readonly string[],
  displayFormat: DisplayFormat,
): string[] {
  switch (displayFormat) {
    case "すべて表示":
      return [...advertiserNames];
    case "すべて表示（逆順）":
      return [...advertiserNames].reverse();
    case "同じ名前をまとめる":
      return dedupeKeepFirst(advertiserNames);
    case "同じ名前をまとめる（逆順）":
      // 重複除去（最初の出現を残す）→ 逆順の順で処理する。
      // 現行Kotlin実装の 逆順→重複除去 はバグであり踏襲しない（FE-104、付録A-13）
      return dedupeKeepFirst(advertiserNames).reverse();
  }
}

/**
 * 広告主名リストを表示形式・敬称・区切り文字・1行文字数で整形する（FE-101〜FE-108, FE-110）。
 * 文字数はUTF-16コード単位（String.length）で数える（FE-105、付録D-7）
 */
export function formatAdvertiserList(
  advertiserNames: readonly string[],
  displayFormat: DisplayFormat,
  honorificSuffix: string,
  nameSeparator: string,
  charsPerLine: number,
): string {
  const orderedNames = applyDisplayFormat(advertiserNames, displayFormat);
  // 最終要素を除く各要素の末尾に区切り文字を付与し、区切りも文字数計算に含める（FE-105）
  const elements = orderedNames.map((name, index) => {
    const withHonorific = `${name}${honorificSuffix}`;
    return index === orderedNames.length - 1 ? withHonorific : `${withHonorific}${nameSeparator}`;
  });

  const lines: string[] = [];
  let currentLine = "";
  for (const element of elements) {
    // 超過かつ現在行が空でない場合のみ改行する。単一要素が上限を超える場合は分割しない（FE-106, FE-108）
    if (currentLine !== "" && currentLine.length + element.length > charsPerLine) {
      lines.push(currentLine);
      currentLine = "";
    }
    currentLine += element;
  }
  if (currentLine !== "") {
    lines.push(currentLine);
  }

  // 各行はtrimして出力する。行末の区切り文字は除去しない（FE-107）。末尾に余分な改行は付けない（FE-110）
  return lines.map((line) => line.trim()).join("\n");
}
