// 広告主リストページ（仕様: FRONTEND_REQUIREMENTS.md FE-070〜FE-095, FE-005）
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { ErrorBox } from "../components/ErrorBox";
import { VideoSearchForm } from "../components/VideoSearchForm";
import {
  fetchNicoadHistory,
  fetchVideoInfo,
  type NicoadHistoryResult,
  olderCachedAt,
  type VideoInfo,
} from "../lib/api";
import { formatDateTimeYmdHms } from "../lib/dateFormat";
import {
  DISPLAY_FORMATS,
  type DisplayFormat,
  formatAdvertiserList,
  HONORIFIC_OPTIONS,
  type HonorificOption,
  parseCharsPerLine,
  resolveHonorific,
} from "../lib/formatAdvertiserList";

/** キャッシュ表示（FE-077〜FE-077c）の算出 */
function cacheDisplay(
  info: VideoInfo | null,
  history: NicoadHistoryResult | null,
): { label: string; datetime: string } | null {
  const entries = [info, history].filter(
    (entry): entry is VideoInfo | NicoadHistoryResult => entry !== null && entry.cachedAt !== null,
  );
  if (entries.length === 0) {
    return null;
  }
  let oldest: string | null = null;
  for (const entry of entries) {
    if (entry.cachedAt !== null) {
      oldest = olderCachedAt(oldest, entry.cachedAt);
    }
  }
  if (oldest === null) {
    return null;
  }
  // cachedAtがnullの側はfromCacheを偽として扱う（FE-077a）
  const infoFromCache = info !== null && info.cachedAt !== null && info.fromCache;
  const historyFromCache = history !== null && history.cachedAt !== null && history.fromCache;
  const label = infoFromCache && historyFromCache ? "利用中" : "更新済";
  return { label, datetime: formatDateTimeYmdHms(oldest) };
}

export function AdvertisersPage() {
  const { videoId } = useParams<{ videoId: string }>();
  const navigate = useNavigate();
  const [info, setInfo] = useState<VideoInfo | null>(null);
  const [history, setHistory] = useState<NicoadHistoryResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 表示設定（FE-090〜FE-094）
  const [honorific, setHonorific] = useState<HonorificOption>("様");
  const [customHonorific, setCustomHonorific] = useState("");
  const [displayFormat, setDisplayFormat] = useState<DisplayFormat>("すべて表示");
  const [charsPerLineInput, setCharsPerLineInput] = useState("50");

  const load = useCallback(
    async (refresh: boolean, isActive: () => boolean) => {
      if (videoId === undefined) {
        return;
      }
      // 両APIを並列に発行し、両方の完了（広告履歴は全チャンク）を待つ（FE-070, FE-070a, FE-075）
      const [infoResult, historyResult] = await Promise.all([
        fetchVideoInfo(videoId, { refresh }),
        fetchNicoadHistory(videoId, { refresh }),
      ]);
      if (!isActive()) {
        return;
      }
      // いずれかが404なら/404へ遷移する（FE-005, FE-072）
      if (
        (!infoResult.ok && infoResult.status === 404) ||
        (!historyResult.ok && historyResult.status === 404)
      ) {
        navigate("/404");
        return;
      }
      // 404以外のエラーは"; "で連結して表示する（FE-073）
      const messages = [infoResult, historyResult]
        .filter((result): result is { ok: false; status: number; message: string } => !result.ok)
        .map((result) => result.message);
      setError(messages.length > 0 ? messages.join("; ") : null);
      if (infoResult.ok) {
        setInfo(infoResult.data);
      }
      if (historyResult.ok) {
        setHistory(historyResult.data);
      }
    },
    [videoId, navigate],
  );

  useEffect(() => {
    let active = true;
    setLoading(true);
    setInfo(null);
    setHistory(null);
    setError(null);
    void load(false, () => active).finally(() => {
      if (active) {
        setLoading(false);
      }
    });
    return () => {
      active = false;
    };
  }, [load]);

  const handleRefresh = () => {
    setRefreshing(true);
    void load(true, () => true).finally(() => setRefreshing(false));
  };

  const advertiserNames = useMemo(
    () => (history === null ? [] : history.histories.map((item) => item.advertiserName)),
    [history],
  );

  // 表示設定の変更は再取得せず即座に反映する（FE-095）
  const formattedList = useMemo(
    () =>
      formatAdvertiserList(
        advertiserNames,
        displayFormat,
        resolveHonorific(honorific, customHonorific),
        parseCharsPerLine(charsPerLineInput),
      ),
    [advertiserNames, displayFormat, honorific, customHonorific, charsPerLineInput],
  );

  const totalContribution = useMemo(
    () =>
      history === null ? 0 : history.histories.reduce((sum, item) => sum + item.contribution, 0),
    [history],
  );

  const cache = cacheDisplay(info, history);
  const hasAnyData = info !== null || history !== null;

  return (
    <div className="content-narrow">
      <h1 className="page-title">ニコニコ動画 広告主リスト取得</h1>
      <p className="page-description">
        動画URLまたは動画IDを入力すると、広告主のリストを整形して表示します。感謝メッセージの作成などにご利用ください。
      </p>
      <div className="card">
        {/* 検索フォームの初期値は現在のvideoId（FE-080） */}
        <VideoSearchForm key={videoId} initialValue={videoId ?? ""} />
      </div>

      {loading && <p className="loading">読み込み中...</p>}
      {error !== null && <ErrorBox message={error} />}

      {hasAnyData && (
        <>
          <button
            type="button"
            className="button button--secondary button--full"
            disabled={loading || refreshing}
            onClick={handleRefresh}
          >
            {refreshing ? "更新中..." : "データを更新"}
          </button>
          {cache !== null && (
            <p className="cache-note">
              キャッシュ: {cache.label} ({cache.datetime})
            </p>
          )}
        </>
      )}

      {info !== null && (
        <section className="card">
          <h2>動画情報</h2>
          <div className="video-info">
            <img className="video-info__thumbnail" src={info.thumbnail} alt={info.title} />
            <div>
              <p className="video-info__title">{info.title}</p>
              <p className="video-info__meta">動画ID: {info.videoId}</p>
              <p className="video-info__meta">投稿者ID: {info.userId}</p>
            </div>
          </div>
        </section>
      )}

      {history !== null && (
        <section className="card">
          <h2>広告主リスト</h2>
          <div className="stats-row">
            <span>
              広告件数: <strong>{history.histories.length}</strong>
            </span>
            <span>
              総貢献度: <strong>{totalContribution}</strong>pt
            </span>
          </div>

          <h2>表示設定</h2>
          <div className="settings-grid">
            <label>
              敬称:
              <select
                className="select-input"
                value={honorific}
                onChange={(event) => setHonorific(event.target.value as HonorificOption)}
              >
                {HONORIFIC_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>
            {honorific === "カスタム" && (
              <label>
                カスタム敬称:
                <input
                  type="text"
                  className="text-input"
                  placeholder="例: 殿"
                  value={customHonorific}
                  onChange={(event) => setCustomHonorific(event.target.value)}
                />
              </label>
            )}
            <label>
              リスト表示形式:
              <select
                className="select-input"
                value={displayFormat}
                onChange={(event) => setDisplayFormat(event.target.value as DisplayFormat)}
              >
                {DISPLAY_FORMATS.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>
            <label>
              1行の文字数:
              <input
                type="text"
                className="text-input chars-input"
                value={charsPerLineInput}
                onChange={(event) => setCharsPerLineInput(event.target.value)}
              />
            </label>
          </div>

          <div className="formatted-list">{formattedList}</div>
          <button
            type="button"
            className="button button--full"
            onClick={() => {
              void navigator.clipboard.writeText(formattedList);
            }}
          >
            クリップボードにコピー
          </button>
        </section>
      )}
    </div>
  );
}
