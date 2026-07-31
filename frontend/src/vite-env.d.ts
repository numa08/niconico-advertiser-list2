/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** GA4測定ID。ビルド時に注入される（未設定なら計測無効。FE-150） */
  readonly GA4_MEASUREMENT_ID?: string;
}
