// 動画カード（仕様: FRONTEND_REQUIREMENTS.md FE-004, FE-037）
import { useNavigate } from "react-router";
import type { UserVideo } from "../lib/api";
import { formatDateYmd } from "../lib/dateFormat";

export function VideoCard({ video }: { video: UserVideo }) {
  const navigate = useNavigate();
  return (
    <button
      type="button"
      className="video-card"
      onClick={() => {
        // 履歴はPUSHで更新する（FE-004）
        navigate(`/advertisers/${video.videoId}`);
      }}
    >
      <img className="video-card__thumbnail" src={video.thumbnail} alt="" loading="lazy" />
      <p className="video-card__title">{video.title}</p>
      <p className="video-card__meta">
        {video.videoId} ・ {formatDateYmd(video.published)}
      </p>
    </button>
  );
}
