import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { app } from "../src/app";

describe("app", () => {
  it("GET /api/health が 200 を返す", async () => {
    const res = await app.request("/api/health", {}, env);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ status: "ok" });
  });

  it("KVバインディングがテスト環境で利用できる", async () => {
    await env.NICO_CACHE.put("smoke-test", "value");
    expect(await env.NICO_CACHE.get("smoke-test")).toBe("value");
  });
});
