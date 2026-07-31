import { Hono } from "hono";

/**
 * APIルーター本体
 *
 * 静的アセット（Kobwebエクスポート成果物）は wrangler.jsonc の assets 設定で
 * 配信されるため、この Worker は /api/* のみを処理する。
 * 各エンドポイントの実装はフェーズ2で追加する。
 */
export const app = new Hono<{ Bindings: Env }>();

app.get("/api/health", (c) => c.json({ status: "ok" }));
