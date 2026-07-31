// 対応仕様: docs/FRONTEND_REQUIREMENTS.md
//   FE-170〜FE-172, FE-171a（localStorage永続化）
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  clearUserId,
  loadColorMode,
  loadUserId,
  saveColorMode,
  saveUserId,
} from "../src/lib/preferences";

/** Mapを裏書きにしたlocalStorageモック */
function stubLocalStorage(initial: Record<string, string> = {}) {
  const store = new Map(Object.entries(initial));
  const storage = {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => store.clear(),
    key: (i: number) => [...store.keys()][i] ?? null,
    get length() {
      return store.size;
    },
  };
  vi.stubGlobal("localStorage", storage);
  return store;
}

/** 読み書きが常に失敗するlocalStorageモック（FE-172） */
function stubBrokenLocalStorage() {
  vi.stubGlobal("localStorage", {
    getItem: () => {
      throw new Error("storage disabled");
    },
    setItem: () => {
      throw new Error("storage disabled");
    },
    removeItem: () => {
      throw new Error("storage disabled");
    },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("投稿者IDの永続化", () => {
  it("FE-170: キー niconico_user_id へ保存・読み出し・削除する", () => {
    const store = stubLocalStorage();

    saveUserId("753685");
    expect(store.get("niconico_user_id")).toBe("753685");
    expect(loadUserId()).toBe("753685");

    clearUserId();
    expect(store.has("niconico_user_id")).toBe(false);
    expect(loadUserId()).toBeNull();
  });

  it("FE-172: 読み書きが失敗しても例外を伝播せず既定値で動作する", () => {
    stubBrokenLocalStorage();

    expect(() => saveUserId("753685")).not.toThrow();
    expect(() => clearUserId()).not.toThrow();
    expect(loadUserId()).toBeNull();
  });
});

describe("カラーモードの永続化", () => {
  it("FE-171: キー niconico_advertiser_list2:colorMode へ3値の文字列として保存する", () => {
    const store = stubLocalStorage();

    saveColorMode("DARK");
    expect(store.get("niconico_advertiser_list2:colorMode")).toBe("DARK");
    expect(loadColorMode()).toBe("DARK");
  });

  it("FE-171: 保存時に他のキー（color-mode-preference等）を併用しない", () => {
    const store = stubLocalStorage();

    saveColorMode("LIGHT");
    expect([...store.keys()]).toEqual(["niconico_advertiser_list2:colorMode"]);
  });

  it("FE-171a: 保存値が既知の3値以外ならSYSTEMとして扱う", () => {
    stubLocalStorage({ "niconico_advertiser_list2:colorMode": "dark-v2" });
    expect(loadColorMode()).toBe("SYSTEM");
  });

  it("FE-171a: 未保存ならSYSTEMとして扱う", () => {
    stubLocalStorage();
    expect(loadColorMode()).toBe("SYSTEM");
  });

  it("FE-172: 読み書きが失敗しても例外を伝播せずSYSTEMで動作する", () => {
    stubBrokenLocalStorage();

    expect(() => saveColorMode("DARK")).not.toThrow();
    expect(loadColorMode()).toBe("SYSTEM");
  });
});
