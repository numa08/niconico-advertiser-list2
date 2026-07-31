// localStorage永続化（仕様: FRONTEND_REQUIREMENTS.md FE-170〜FE-172, FE-171a）
// TODO: TDDレッドフェーズのスタブ。テスト(test/preferences.test.ts)を満たす実装に置き換える

export const USER_ID_STORAGE_KEY = "niconico_user_id";
export const COLOR_MODE_STORAGE_KEY = "niconico_advertiser_list2:colorMode";

export const COLOR_MODES = ["LIGHT", "DARK", "SYSTEM"] as const;
export type ColorMode = (typeof COLOR_MODES)[number];

/** 保存済みの投稿者IDを返す。未保存・読み取り失敗時はnull（FE-170, FE-172） */
export function loadUserId(): string | null {
  return null;
}

export function saveUserId(userId: string): void {
  void userId;
}

export function clearUserId(): void {}

/** 保存済みのカラーモードを返す。未保存・不正値・読み取り失敗時はSYSTEM（FE-171, FE-171a, FE-172） */
export function loadColorMode(): ColorMode {
  return "SYSTEM";
}

export function saveColorMode(mode: ColorMode): void {
  void mode;
}
