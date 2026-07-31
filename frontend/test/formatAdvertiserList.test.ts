// 対応仕様: docs/FRONTEND_REQUIREMENTS.md
//   FE-100〜FE-110（整形アルゴリズム）、付録D-1〜D-7（テストベクタ）
import { describe, expect, it } from "vitest";
import {
  formatAdvertiserList,
  parseCharsPerLine,
  resolveHonorific,
} from "../src/lib/formatAdvertiserList";

describe("resolveHonorific", () => {
  it("FE-100: カスタム以外は選択値そのものを返す", () => {
    expect(resolveHonorific("様", "")).toBe("様");
    expect(resolveHonorific("さん", "無視される")).toBe("さん");
    expect(resolveHonorific("くん", "")).toBe("くん");
  });

  it("FE-100/D-6: カスタムはカスタム入力値を返す", () => {
    expect(resolveHonorific("カスタム", "殿")).toBe("殿");
  });

  it("FE-100/D-6: カスタム入力が空文字なら空文字（敬称なし）", () => {
    expect(resolveHonorific("カスタム", "")).toBe("");
  });
});

describe("parseCharsPerLine", () => {
  it("FE-109/D-5: 整数として解釈できる値はそのまま使う", () => {
    expect(parseCharsPerLine("50")).toBe(50);
  });

  it("FE-109/D-5: 空欄は既定値50", () => {
    expect(parseCharsPerLine("")).toBe(50);
  });

  it("FE-109/D-5: 非数値は既定値50", () => {
    expect(parseCharsPerLine("abc")).toBe(50);
  });

  it("FE-109/D-5: 0は1にクランプ", () => {
    expect(parseCharsPerLine("0")).toBe(1);
  });

  it("FE-109/D-5: 負数は1にクランプ", () => {
    expect(parseCharsPerLine("-5")).toBe(1);
  });
});

describe("formatAdvertiserList", () => {
  it("FE-105〜FE-107/D-1: 基本的な折り返し", () => {
    const result = formatAdvertiserList(["あ様", "い様", "う様", "え様"], "すべて表示", "", 6);
    expect(result).toBe("あ様、い様、\nう様、え様");
  });

  it("FE-108/D-2: 1行の文字数を超える単一要素は分割しない", () => {
    const result = formatAdvertiserList(["あいうえおかきくけこ"], "すべて表示", "", 1);
    expect(result).toBe("あいうえおかきくけこ");
  });

  it("FE-101: すべて表示はAPIの返却順のまま敬称を連結する", () => {
    expect(formatAdvertiserList(["B", "A"], "すべて表示", "様", 50)).toBe("B様、A様");
  });

  it("FE-102: すべて表示（逆順）は全件を逆順にする", () => {
    expect(formatAdvertiserList(["A", "B"], "すべて表示（逆順）", "", 50)).toBe("B、A");
  });

  it("FE-103/D-4: 同じ名前をまとめるは最初の出現を残す重複除去", () => {
    expect(formatAdvertiserList(["A", "B", "A", "C"], "同じ名前をまとめる", "", 50)).toBe(
      "A、B、C",
    );
  });

  it("FE-104/D-3: 同じ名前をまとめる（逆順）は重複除去→逆順の順で処理する", () => {
    // 現行Kotlin実装(逆順→重複除去)とは意図的に異なる（付録A-13）
    const result = formatAdvertiserList(
      ["A", "B", "A", "C"],
      "同じ名前をまとめる（逆順）",
      "様",
      50,
    );
    expect(result).toBe("C様、B様、A様");
  });

  it("FE-105/D-7: 文字数はUTF-16コード単位で数える（サロゲートペア）", () => {
    const result = formatAdvertiserList(["🎉様", "🎉様"], "すべて表示", "", 5);
    expect(result).toBe("🎉様、\n🎉様");
  });

  it("FE-110: 末尾に余分な改行を付加しない", () => {
    expect(formatAdvertiserList(["A"], "すべて表示", "様", 50)).toBe("A様");
    expect(formatAdvertiserList(["A"], "すべて表示", "様", 50).endsWith("\n")).toBe(false);
  });

  it("FE-107: 行末の区切り文字「、」は除去しない", () => {
    const result = formatAdvertiserList(["あ様", "い様", "う様"], "すべて表示", "", 3);
    const lines = result.split("\n");
    expect(lines[0]).toBe("あ様、");
    expect(lines[1]).toBe("い様、");
    expect(lines[2]).toBe("う様");
  });
});
