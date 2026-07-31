// GA4計測（仕様: FRONTEND_REQUIREMENTS.md FE-150〜FE-153）
// gtagが利用できない環境では例外を発生させず無視する（FE-153）

declare global {
  interface Window {
    dataLayer?: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

/**
 * GA4を初期化する（FE-150）。
 * 測定IDがビルド時に設定されていない場合は何もしない。
 * SPA遷移を自前で計測するため send_page_view は無効にする
 */
export function initAnalytics(): void {
  const measurementId = import.meta.env.GA4_MEASUREMENT_ID;
  if (measurementId === undefined || measurementId === "") {
    return;
  }
  const script = document.createElement("script");
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${measurementId}`;
  document.head.appendChild(script);

  window.dataLayer = window.dataLayer ?? [];
  window.gtag = (...args: unknown[]) => {
    window.dataLayer?.push(args);
  };
  window.gtag("js", new Date());
  window.gtag("config", measurementId, { send_page_view: false });
}

function safeGtag(...args: unknown[]): void {
  try {
    if (typeof window.gtag === "function") {
      window.gtag(...args);
    }
  } catch {
    // 計測の失敗はアプリの動作に影響させない（FE-153）
  }
}

/** ページビューを送信する。初期化時とクライアント側ルート遷移時に呼ぶ（FE-151） */
export function trackPageView(pagePath: string, pageTitle: string): void {
  safeGtag("event", "page_view", { page_path: pagePath, page_title: pageTitle });
}

/** 動画検索イベント（FE-062, FE-152） */
export function trackVideoSearch(): void {
  safeGtag("event", "video_search", {});
}

/** 投稿者ID設定イベント（FE-054, FE-152） */
export function trackUserIdSet(): void {
  safeGtag("event", "user_id_set", {});
}

/** 投稿者ID解除イベント（FE-058, FE-152） */
export function trackUserIdClear(): void {
  safeGtag("event", "user_id_clear", {});
}
