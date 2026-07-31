// 対応仕様: docs/FRONTEND_REQUIREMENTS.md
//   FE-016（投稿者アイコンURL）、付録D-8（テストベクタ）、付録E（実測根拠）
import { describe, expect, it } from "vitest";
import { userIconUrl } from "../src/lib/userIconUrl";

const BASE = "https://secure-dcdn.cdn.nimg.jp/nicoaccount/usericon";

describe("userIconUrl", () => {
  it("FE-016/D-8: prefixは floor(userId / 10000)", () => {
    expect(userIconUrl("753685")).toBe(`${BASE}/75/753685.jpg`);
    expect(userIconUrl("1594318")).toBe(`${BASE}/159/1594318.jpg`);
    expect(userIconUrl("13647798")).toBe(`${BASE}/1364/13647798.jpg`);
    expect(userIconUrl("134979178")).toBe(`${BASE}/13497/134979178.jpg`);
  });

  it("FE-016: 5桁未満のIDはprefixが0になる", () => {
    // floor(4 / 10000) = 0
    expect(userIconUrl("4")).toBe(`${BASE}/0/4.jpg`);
  });
});
