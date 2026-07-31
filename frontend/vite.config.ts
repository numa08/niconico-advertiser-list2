import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
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
