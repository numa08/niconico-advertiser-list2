// 投稿者ID設定ダイアログ（仕様: FRONTEND_REQUIREMENTS.md FE-050〜FE-059）
import { useEffect, useState } from "react";
import { useUserId } from "../context/UserIdContext";
import { useUserIdDialog } from "../context/UserIdDialogContext";
import { trackUserIdClear, trackUserIdSet } from "../lib/analytics";
import { extractUserId } from "../lib/userIdExtractor";

export function UserIdSettingDialog() {
  const { userId, setUserId, removeUserId } = useUserId();
  const { isOpen, closeDialog } = useUserIdDialog();
  const [input, setInput] = useState("");
  const [error, setError] = useState<string | null>(null);

  // ダイアログが開かれたとき、保存済みIDを初期値にしエラーを消す（FE-053）
  useEffect(() => {
    if (isOpen) {
      setInput(userId ?? "");
      setError(null);
    }
  }, [isOpen, userId]);

  if (!isOpen) {
    return null;
  }

  const handleSave = () => {
    const extracted = extractUserId(input);
    if (extracted === null) {
      // 抽出できない場合はエラーを表示し、閉じない（FE-055）
      setError("有効なユーザーIDまたはURLを入力してください");
      return;
    }
    setUserId(extracted);
    trackUserIdSet();
    closeDialog();
  };

  const handleClear = () => {
    removeUserId();
    trackUserIdClear();
    closeDialog();
  };

  return (
    // オーバーレイ（ダイアログ本体の外側）のクリックで閉じる（FE-051）。
    // target===currentTarget判定によりダイアログ本体内のクリックでは発火しない。
    // クリックは補助操作でありキーボードはEscキー・各ボタンで完結するため、静的要素への
    // ハンドラ付与を許容する
    // biome-ignore lint/a11y/noStaticElementInteractions: クリック外し閉じは補助操作（Esc/ボタンで代替可能）
    <div
      className="dialog-overlay"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          closeDialog();
        }
      }}
      onKeyDown={(event) => {
        if (event.key === "Escape") {
          closeDialog();
        }
      }}
    >
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="user-id-dialog-title"
      >
        <h2 id="user-id-dialog-title">投稿者IDを設定</h2>
        <p>あなたのニコニコ動画のユーザーIDまたはユーザーページURLを入力してください</p>
        <p>例: 753685 または https://www.nicovideo.jp/user/753685</p>
        <input
          type="text"
          className="text-input"
          placeholder="ユーザーID または URL"
          value={input}
          onChange={(event) => {
            setInput(event.target.value);
            // 入力の編集でエラーメッセージを消す（FE-056）
            setError(null);
          }}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              handleSave();
            }
          }}
        />
        {error !== null && <p className="form-error">{error}</p>}
        <div className="dialog__buttons">
          {userId !== null && (
            <button type="button" className="button button--danger" onClick={handleClear}>
              ユーザーID設定を解除
            </button>
          )}
          <button type="button" className="button button--secondary" onClick={closeDialog}>
            キャンセル
          </button>
          <button type="button" className="button" onClick={handleSave}>
            保存
          </button>
        </div>
      </div>
    </div>
  );
}
