import { cloudflareTest } from "@cloudflare/vitest-pool-workers";
import { defineConfig } from "vitest/config";

// wrangler.jsonc の assets はビルド成果物（未生成のことがある）を参照するため、
// テストは wrangler 設定を読まず miniflare にバインディングを直接定義する
export default defineConfig({
  plugins: [
    cloudflareTest({
      miniflare: {
        compatibilityDate: "2026-07-01",
        compatibilityFlags: ["nodejs_compat"],
        kvNamespaces: ["NICO_CACHE"],
      },
    }),
  ],
});
