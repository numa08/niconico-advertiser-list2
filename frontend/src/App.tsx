// アプリのルーティングと共通レイアウト（仕様: FRONTEND_REQUIREMENTS.md FE-001〜FE-006, FE-010, FE-151）
import { useEffect } from "react";
import { Route, Routes, useLocation } from "react-router";
import { Footer } from "./components/Footer";
import { Header } from "./components/Header";
import { UserIdSettingDialog } from "./components/UserIdSettingDialog";
import { ColorModeProvider } from "./context/ColorModeContext";
import { UserIdProvider } from "./context/UserIdContext";
import { UserIdDialogProvider } from "./context/UserIdDialogContext";
import { trackPageView } from "./lib/analytics";
import { AdvertisersPage } from "./pages/AdvertisersPage";
import { HomePage } from "./pages/HomePage";
import { NotFoundPage } from "./pages/NotFoundPage";

/** 初期化時とクライアント側ルート遷移のたびにpage_viewを送信する（FE-151） */
function PageViewTracker() {
  const location = useLocation();
  useEffect(() => {
    trackPageView(location.pathname, document.title);
  }, [location]);
  return null;
}

export function App() {
  return (
    <ColorModeProvider>
      <UserIdProvider>
        <UserIdDialogProvider>
          <PageViewTracker />
          <div className="app">
            <Header />
            <main className="app-main">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/advertisers/:videoId" element={<AdvertisersPage />} />
                <Route path="/404" element={<NotFoundPage />} />
                {/* 未知のパスは404画面（FE-006。配信側SPAフォールバックの前提） */}
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </main>
            <Footer />
          </div>
          <UserIdSettingDialog />
        </UserIdDialogProvider>
      </UserIdProvider>
    </ColorModeProvider>
  );
}
