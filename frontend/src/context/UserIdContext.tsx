// 投稿者IDのアプリ全体共有状態（仕様: FRONTEND_REQUIREMENTS.md FE-017, FE-040）
// 現行実装はヘッダーとトップページが状態を独立保持していたため即時反映されなかった（付録A-1）。
// 単一のContextに統合し、保存・解除を全画面へ即座に伝播させる
import { createContext, type ReactNode, useCallback, useContext, useState } from "react";
import { clearUserId, loadUserId, saveUserId } from "../lib/preferences";

interface UserIdContextValue {
  userId: string | null;
  setUserId: (userId: string) => void;
  removeUserId: () => void;
}

const UserIdContext = createContext<UserIdContextValue | null>(null);

export function UserIdProvider({ children }: { children: ReactNode }) {
  const [userId, setUserIdState] = useState<string | null>(loadUserId);

  const setUserId = useCallback((next: string) => {
    saveUserId(next);
    setUserIdState(next);
  }, []);

  const removeUserId = useCallback(() => {
    clearUserId();
    setUserIdState(null);
  }, []);

  return (
    <UserIdContext.Provider value={{ userId, setUserId, removeUserId }}>
      {children}
    </UserIdContext.Provider>
  );
}

export function useUserId(): UserIdContextValue {
  const value = useContext(UserIdContext);
  if (value === null) {
    throw new Error("useUserId must be used within UserIdProvider");
  }
  return value;
}
