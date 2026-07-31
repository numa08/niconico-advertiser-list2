// 投稿者ID設定ダイアログの開閉状態（仕様: FRONTEND_REQUIREMENTS.md FE-013, FE-031a）
// ヘッダーのアイコンとトップページのボタンの両方から開けるようContextで公開する
import { createContext, type ReactNode, useCallback, useContext, useState } from "react";

interface UserIdDialogContextValue {
  isOpen: boolean;
  openDialog: () => void;
  closeDialog: () => void;
}

const UserIdDialogContext = createContext<UserIdDialogContextValue | null>(null);

export function UserIdDialogProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const openDialog = useCallback(() => setIsOpen(true), []);
  const closeDialog = useCallback(() => setIsOpen(false), []);
  return (
    <UserIdDialogContext.Provider value={{ isOpen, openDialog, closeDialog }}>
      {children}
    </UserIdDialogContext.Provider>
  );
}

export function useUserIdDialog(): UserIdDialogContextValue {
  const value = useContext(UserIdDialogContext);
  if (value === null) {
    throw new Error("useUserIdDialog must be used within UserIdDialogProvider");
  }
  return value;
}
