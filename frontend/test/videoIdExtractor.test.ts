// 対応仕様: docs/FRONTEND_REQUIREMENTS.md
//   FE-120〜FE-124（動画ID抽出）
// テストケースは現行KotlinのVideoIdExtractorTest（受け入れ基準）を移植したもの
import { describe, expect, it } from "vitest";
import { extractVideoId } from "../src/lib/videoIdExtractor";

describe("extractVideoId", () => {
  it("FE-122: 直接の動画IDはそのまま返す", () => {
    expect(extractVideoId("sm12345678")).toBe("sm12345678");
    expect(extractVideoId("sm1")).toBe("sm1");
  });

  it("FE-120: 前後の空白を除去してから判定する", () => {
    expect(extractVideoId("  sm12345678  ")).toBe("sm12345678");
  });

  it("FE-123: nicovideo.jpのwatch URLから動画IDを抽出する", () => {
    expect(extractVideoId("https://www.nicovideo.jp/watch/sm12345678")).toBe("sm12345678");
    expect(extractVideoId("https://nicovideo.jp/watch/sm12345678")).toBe("sm12345678");
    expect(extractVideoId("http://www.nicovideo.jp/watch/sm12345678")).toBe("sm12345678");
    expect(extractVideoId("https://sp.nicovideo.jp/watch/sm12345678")).toBe("sm12345678");
    expect(extractVideoId("https://www.nicovideo.jp/watch/sm1")).toBe("sm1");
  });

  it("FE-123: ホストの判定は大文字小文字を区別しない", () => {
    expect(extractVideoId("https://WWW.NICOVIDEO.JP/watch/sm12345678")).toBe("sm12345678");
  });

  it("FE-123: クエリパラメータ・フラグメントを除いた動画IDを返す", () => {
    expect(extractVideoId("https://www.nicovideo.jp/watch/sm12345678?ref=ranking")).toBe(
      "sm12345678",
    );
    expect(extractVideoId("https://www.nicovideo.jp/watch/sm12345678#comment")).toBe("sm12345678");
    expect(extractVideoId("https://www.nicovideo.jp/watch/sm12345678?ref=ranking#comment")).toBe(
      "sm12345678",
    );
  });

  it("FE-124: 空・空白のみは失敗する", () => {
    expect(extractVideoId("")).toBeNull();
    expect(extractVideoId("   ")).toBeNull();
  });

  it("FE-121/FE-124: 動画ID形式でない直接入力は失敗する", () => {
    expect(extractVideoId("sm")).toBeNull();
    expect(extractVideoId("smabc")).toBeNull();
    expect(extractVideoId("SM12345678")).toBeNull();
    expect(extractVideoId("12345678")).toBeNull();
  });

  it("FE-124: スキームなし・別ドメイン・接尾一致ドメインは失敗する", () => {
    expect(extractVideoId("www.nicovideo.jp/watch/sm12345678")).toBeNull();
    expect(extractVideoId("https://example.com/watch/sm12345678")).toBeNull();
    expect(extractVideoId("https://fakenicovideo.jp/watch/sm12345678")).toBeNull();
  });

  it("FE-124: /watch/以外のパスは失敗する", () => {
    expect(extractVideoId("https://www.nicovideo.jp/user/12345")).toBeNull();
    expect(extractVideoId("https://www.nicovideo.jp/")).toBeNull();
  });

  it("FE-124: URL中の動画IDが不正な場合は失敗する", () => {
    expect(extractVideoId("https://www.nicovideo.jp/watch/sm")).toBeNull();
    expect(extractVideoId("https://www.nicovideo.jp/watch/smabc")).toBeNull();
  });
});
