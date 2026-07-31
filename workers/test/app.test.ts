// 対応仕様: docs/WORKERS_API_SPEC.md
//   NF-5（死活監視エンドポイント）
//   NF-3（予期しない例外の最終防衛線 onError: 500応答と構造化ログ）
import { env } from "cloudflare:workers";
import { Hono } from "hono";
import { afterEach, describe, expect, it, vi } from "vitest";
import { app, handleUnexpectedError } from "../src/app";
import { expectJson, mockNicoFetch } from "./helpers";

afterEach(() => {
  vi.restoreAllMocks();
});

/** ハンドラのtry/catchを通らない例外を再現するため、意図的にthrowするルートを持つアプリを作る */
function throwingApp(path: string): Hono<{ Bindings: Env }> {
  const testApp = new Hono<{ Bindings: Env }>();
  testApp.get(path, () => {
    throw new Error("unexpected failure");
  });
  testApp.onError(handleUnexpectedError);
  return testApp;
}

describe("app", () => {
  it("NF-5: GET /api/health はニコニコへアクセスせず200を返す", async () => {
    const recorded = mockNicoFetch([]);
    const res = await app.request("/api/health", {}, env);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ status: "ok" });
    expect(recorded.length).toBe(0);
  });

  it("KVバインディングがテスト環境で利用できる", async () => {
    await env.NICO_CACHE.put("smoke-test", "value");
    expect(await env.NICO_CACHE.get("smoke-test")).toBe("value");
  });

  it("NF-3: 予期しない例外は500(JSON)と構造化ログを出力する", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const res = await throwingApp("/api/video/boom").request("/api/video/boom", {}, env);
    const body = await expectJson(res, 500);
    expect(body.error).toBe("unexpected failure");

    expect(consoleSpy).toHaveBeenCalledTimes(1);
    const logged = JSON.parse(consoleSpy.mock.calls[0]![0] as string) as Record<
      string,
      unknown
    >;
    expect(logged.level).toBe("error");
    expect(logged.context).toBe("app.onError");
    expect(logged.method).toBe("GET");
    expect(logged.path).toBe("/api/video/boom");
    expect(logged.message).toBe("unexpected failure");
    expect(typeof logged.stack).toBe("string");
  });

  it("NF-3: /api/user/* の予期しない例外は500(テキスト)を返す（エラー形式の現行互換）", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const res = await throwingApp("/api/user/boom").request("/api/user/boom", {}, env);
    expect(res.status).toBe(500);
    expect(await res.text()).toBe("Internal server error: unexpected failure");
    expect(consoleSpy).toHaveBeenCalledTimes(1);
  });
});
