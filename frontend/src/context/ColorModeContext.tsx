// カラーモード管理（仕様: FRONTEND_REQUIREMENTS.md FE-020〜FE-023）
// 選好(LIGHT/DARK/SYSTEM)を単一のlocalStorageキーに永続化し（FE-022, FE-171）、
// SYSTEM選択中はprefers-color-schemeに追従する（FE-023）
import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from "react";
import { type ColorMode, loadColorMode, saveColorMode } from "../lib/preferences";

interface ColorModeContextValue {
  preference: ColorMode;
  setPreference: (mode: ColorMode) => void;
}

const ColorModeContext = createContext<ColorModeContextValue | null>(null);

const DARK_SCHEME_QUERY = "(prefers-color-scheme: dark)";

function resolveTheme(preference: ColorMode): "light" | "dark" {
  if (preference === "SYSTEM") {
    return window.matchMedia(DARK_SCHEME_QUERY).matches ? "dark" : "light";
  }
  return preference === "DARK" ? "dark" : "light";
}

export function ColorModeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ColorMode>(loadColorMode);

  useEffect(() => {
    const apply = () => {
      document.documentElement.dataset.theme = resolveTheme(preference);
    };
    apply();
    if (preference !== "SYSTEM") {
      return;
    }
    const mediaQuery = window.matchMedia(DARK_SCHEME_QUERY);
    mediaQuery.addEventListener("change", apply);
    return () => mediaQuery.removeEventListener("change", apply);
  }, [preference]);

  const setPreference = useCallback((mode: ColorMode) => {
    saveColorMode(mode);
    setPreferenceState(mode);
  }, []);

  return (
    <ColorModeContext.Provider value={{ preference, setPreference }}>
      {children}
    </ColorModeContext.Provider>
  );
}

export function useColorMode(): ColorModeContextValue {
  const value = useContext(ColorModeContext);
  if (value === null) {
    throw new Error("useColorMode must be used within ColorModeProvider");
  }
  return value;
}
