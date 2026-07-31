// 動画検索フォーム（仕様: FRONTEND_REQUIREMENTS.md FE-060〜FE-063, FE-003）
import { useState } from "react";
import { useNavigate } from "react-router";
import { trackVideoSearch } from "../lib/analytics";
import { extractVideoId } from "../lib/videoIdExtractor";

export function VideoSearchForm({ initialValue = "" }: { initialValue?: string }) {
  const navigate = useNavigate();
  const [input, setInput] = useState(initialValue);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (event: { preventDefault: () => void }) => {
    event.preventDefault();
    const videoId = extractVideoId(input);
    if (videoId === null) {
      // 抽出できない場合はエラーを表示し、遷移しない（FE-063）
      setError("有効な動画IDまたはURLを入力してください");
      return;
    }
    setError(null);
    trackVideoSearch();
    // 履歴はPUSHで更新する（FE-003。現行のREPLACEは不具合として扱う）
    navigate(`/advertisers/${videoId}`);
  };

  return (
    <form onSubmit={handleSubmit}>
      <label className="field-label" htmlFor="video-search-input">
        動画URLまたは動画IDを入力
      </label>
      <div className="search-form__row">
        <input
          id="video-search-input"
          type="text"
          className="text-input"
          placeholder="例: sm12345678 または https://..."
          value={input}
          onChange={(event) => setInput(event.target.value)}
        />
        <button type="submit" className="button">
          検索
        </button>
      </div>
      {error !== null && <p className="form-error">{error}</p>}
    </form>
  );
}
