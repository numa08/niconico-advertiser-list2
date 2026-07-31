import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  // GA4測定IDは現行(Kobweb/Koyeb)と同じ環境変数名 GA4_MEASUREMENT_ID で注入できるようにする
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
