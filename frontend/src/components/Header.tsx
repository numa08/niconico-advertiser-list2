// ヘッダー（仕様: FRONTEND_REQUIREMENTS.md FE-011〜FE-014, FE-016, FE-018）
import { useEffect, useState } from "react";
import { Link } from "react-router";
import { useUserId } from "../context/UserIdContext";
import { useUserIdDialog } from "../context/UserIdDialogContext";
import { userIconUrl } from "../lib/userIconUrl";
import { ColorModeToggle } from "./ColorModeToggle";
import { PersonIcon } from "./PersonIcon";

export function Header() {
  const { userId } = useUserId();
  const { openDialog } = useUserIdDialog();
  // アイコン画像の読み込み失敗時は人型アイコンへフォールバックする（FE-018）
  const [iconFailed, setIconFailed] = useState(false);

  // 投稿者IDが変わったら失敗状態をリセットして新しいアイコンの読み込みを試す
  useEffect(() => {
    if (userId !== null) {
      setIconFailed(false);
    }
  }, [userId]);

  const showUserIcon = userId !== null && !iconFailed;

  return (
    <header className="app-header">
      <Link to="/" className="app-header__title">
        ニコニコ動画 広告主リスト取得
      </Link>
      <div className="app-header__actions">
        <ColorModeToggle />
        <button
          type="button"
          className="user-icon-button"
          onClick={openDialog}
          aria-label="投稿者IDを設定"
        >
          {showUserIcon ? (
            <img
              src={userIconUrl(userId)}
              alt="投稿者アイコン"
              onError={() => setIconFailed(true)}
            />
          ) : (
            <PersonIcon />
          )}
        </button>
      </div>
    </header>
  );
}
