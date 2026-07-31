import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  // GA4測定IDは環境変数 GA4_MEASUREMENT_ID でビルド時に注入する
  envPrefix: ["VITE_", "GA4_"],
  server: {
    // ローカル開発時はバックエンド(wrangler dev)へAPIをプロキシする
    proxy: {
      "/api": "http://localhost:8787",
    },
  },
  test: {
    include: ["test/**/*.test.ts", "test/**/*.test.tsx"],
  },
});
