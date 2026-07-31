// トップページ（仕様: FRONTEND_REQUIREMENTS.md FE-030〜FE-040）
import { useEffect, useRef, useState } from "react";
import { Pagination } from "../components/Pagination";
import { VideoCard } from "../components/VideoCard";
import { VideoSearchForm } from "../components/VideoSearchForm";
import { useUserId } from "../context/UserIdContext";
import { useUserIdDialog } from "../context/UserIdDialogContext";
import { fetchUserVideos, type UserVideosResult } from "../lib/api";

export function HomePage() {
  const { userId } = useUserId();
  const { openDialog } = useUserIdDialog();
  const [page, setPage] = useState(1);
  const [result, setResult] = useState<UserVideosResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const previousUserId = useRef(userId);

  // 投稿者IDが保存・解除されたらページを1に戻し、保持している一覧を破棄する（FE-040）
  useEffect(() => {
    if (previousUserId.current !== userId) {
      previousUserId.current = userId;
      setPage(1);
      setResult(null);
      setError(null);
    }
  }, [userId]);

  // 投稿者ID・ページの変化で取得する（FE-033）。進行中のリクエストは中断する（FE-033a）
  useEffect(() => {
    if (userId === null) {
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchUserVideos(userId, page, { signal: controller.signal }).then((response) => {
      // 中断されたリクエストの結果は画面に反映しない（FE-033a）
      if (controller.signal.aborted) {
        return;
      }
      if (response.ok) {
        setResult(response.data);
      } else {
        setError(response.message);
      }
      setLoading(false);
    });
    return () => controller.abort();
  }, [userId, page]);

  const handlePageChange = (nextPage: number) => {
    setPage(nextPage);
    // ページ切り替え時は最上部へ戻す（FE-039）
    window.scrollTo(0, 0);
  };

  return (
    <div>
      <div className="content-narrow">
        <h1 className="page-title">ニコニコ動画 広告主リスト取得</h1>
        <p className="page-description">
          動画URLまたは動画IDを入力すると、広告主のリストを整形して表示します。感謝メッセージの作成などにご利用ください。
        </p>
        {/* 検索フォームは投稿者IDの設定有無にかかわらず常に表示する（FE-031） */}
        <div className="card">
          <VideoSearchForm />
        </div>
        {userId === null && (
          <>
            <div className="divider-label">または</div>
            <button
              type="button"
              className="button button--secondary button--full"
              onClick={openDialog}
            >
              投稿者IDを設定して自分の動画から選択
            </button>
            <p className="hint-text">投稿者IDを設定すると、あなたの動画一覧から選択できます</p>
          </>
        )}
      </div>

      {userId !== null && (
        <div className="content-wide">
          {loading && <p className="loading">読み込み中...</p>}
          {error !== null && <p className="inline-error">{error}</p>}
          {!loading && error === null && result !== null && (
            <>
              <p>{result.videosCount}件の動画</p>
              {result.videos.length === 0 ? (
                <p>動画が見つかりませんでした</p>
              ) : (
                <div className="video-grid">
                  {result.videos.map((video) => (
                    <VideoCard key={video.videoId} video={video} />
                  ))}
                </div>
              )}
              <Pagination page={page} hasNext={result.hasNext} onPageChange={handlePageChange} />
            </>
          )}
        </div>
      )}
    </div>
  );
}
