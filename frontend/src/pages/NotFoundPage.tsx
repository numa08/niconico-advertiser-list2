// 404ページ（仕様: FRONTEND_REQUIREMENTS.md FE-180, FE-181, FE-006）
import { VideoSearchForm } from "../components/VideoSearchForm";

export function NotFoundPage() {
  return (
    <div className="content-narrow">
      <h1 className="page-title">ニコニコ動画 広告主リスト取得</h1>
      <p className="page-description">
        動画URLまたは動画IDを入力すると、広告主のリストを整形して表示します。感謝メッセージの作成などにご利用ください。
      </p>
      <div className="card">
        <VideoSearchForm />
      </div>
      <p className="not-found-code">404</p>
      <div className="card" style={{ textAlign: "center" }}>
        <h2>動画が見つかりませんでした</h2>
        <p>指定された動画が存在しないか、URLが間違っている可能性があります</p>
      </div>
    </div>
  );
}
