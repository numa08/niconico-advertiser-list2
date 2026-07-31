// localStorage永続化（仕様: FRONTEND_REQUIREMENTS.md FE-170〜FE-172, FE-171a）
// 読み書きの失敗（プライベートモード等）は例外を伝播させず既定値で継続する（FE-172）

export const USER_ID_STORAGE_KEY = "niconico_user_id";
export const COLOR_MODE_STORAGE_KEY = "niconico_advertiser_list2:colorMode";

export const COLOR_MODES = ["LIGHT", "DARK", "SYSTEM"] as const;
export type ColorMode = (typeof COLOR_MODES)[number];

/** 保存済みの投稿者IDを返す。未保存・読み取り失敗時はnull（FE-170, FE-172） */
export function loadUserId(): string | null {
  try {
    return localStorage.getItem(USER_ID_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function saveUserId(userId: string): void {
  try {
    localStorage.setItem(USER_ID_STORAGE_KEY, userId);
  } catch {
    // 保存できなくても動作は継続する（FE-172）
  }
}

export function clearUserId(): void {
  try {
    localStorage.removeItem(USER_ID_STORAGE_KEY);
  } catch {
    // 削除できなくても動作は継続する（FE-172）
  }
}

function isColorMode(value: string | null): value is ColorMode {
  return value !== null && (COLOR_MODES as readonly string[]).includes(value);
}

/** 保存済みのカラーモードを返す。未保存・不正値・読み取り失敗時はSYSTEM（FE-171, FE-171a, FE-172） */
export function loadColorMode(): ColorMode {
  try {
    const stored = localStorage.getItem(COLOR_MODE_STORAGE_KEY);
    return isColorMode(stored) ? stored : "SYSTEM";
  } catch {
    return "SYSTEM";
  }
}

export function saveColorMode(mode: ColorMode): void {
  try {
    localStorage.setItem(COLOR_MODE_STORAGE_KEY, mode);
  } catch {
    // 保存できなくても動作は継続する（FE-172）
  }
}
