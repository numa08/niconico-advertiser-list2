// 対応仕様: docs/FRONTEND_REQUIREMENTS.md
//   FE-130〜FE-134（投稿者ID抽出）
// テストケースは現行KotlinのUserIdExtractorTest（受け入れ基準）を移植したもの
import { describe, expect, it } from "vitest";
import { extractUserId } from "../src/lib/userIdExtractor";

describe("extractUserId", () => {
  it("FE-132: 直接の投稿者IDはそのまま返す", () => {
    expect(extractUserId("753685")).toBe("753685");
    expect(extractUserId("123456")).toBe("123456");
  });

  it("FE-130: 前後の空白を除去してから判定する", () => {
    expect(extractUserId("  753685  ")).toBe("753685");
  });

  it("FE-133: ユーザーページURLから投稿者IDを抽出する", () => {
    expect(extractUserId("https://www.nicovideo.jp/user/753685")).toBe("753685");
    expect(extractUserId("http://www.nicovideo.jp/user/753685")).toBe("753685");
    expect(extractUserId("https://sp.nicovideo.jp/user/753685")).toBe("753685");
  });

  it("FE-133: /user/{id}/video や RSS URL・フラグメント付きも受理する", () => {
    expect(extractUserId("https://www.nicovideo.jp/user/753685/video")).toBe("753685");
    expect(extractUserId("https://www.nicovideo.jp/user/753685/video?rss=atom")).toBe("753685");
    expect(extractUserId("https://www.nicovideo.jp/user/753685#tab")).toBe("753685");
  });

  it("FE-131/FE-134: 数字のみでない直接入力は失敗する", () => {
    expect(extractUserId("abc")).toBeNull();
    expect(extractUserId("sm12345678")).toBeNull();
  });

  it("FE-134: 空・空白のみは失敗する", () => {
    expect(extractUserId("")).toBeNull();
    expect(extractUserId("   ")).toBeNull();
  });

  it("FE-134: 別ドメイン・/user/以外のパス・英数字混在IDは失敗する", () => {
    expect(extractUserId("https://example.com/user/753685")).toBeNull();
    expect(extractUserId("https://www.nicovideo.jp/watch/sm12345678")).toBeNull();
    expect(extractUserId("https://www.nicovideo.jp/")).toBeNull();
    expect(extractUserId("https://www.nicovideo.jp/user/abc123")).toBeNull();
  });
});
