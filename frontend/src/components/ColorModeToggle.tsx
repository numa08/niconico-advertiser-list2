// カラーモード切り替え（仕様: FRONTEND_REQUIREMENTS.md FE-021〜FE-023）
// 3択を提供し、現在の選択はselectの選択状態として視覚的に区別される
import { useColorMode } from "../context/ColorModeContext";
import { COLOR_MODES, type ColorMode } from "../lib/preferences";

const MODE_LABELS: Record<ColorMode, string> = {
  LIGHT: "ライト",
  DARK: "ダーク",
  SYSTEM: "システム設定に従う",
};

export function ColorModeToggle() {
  const { preference, setPreference } = useColorMode();
  return (
    <select
      className="color-mode-select"
      value={preference}
      onChange={(event) => setPreference(event.target.value as ColorMode)}
      aria-label="カラーモード"
    >
      {COLOR_MODES.map((mode) => (
        <option key={mode} value={mode}>
          {MODE_LABELS[mode]}
        </option>
      ))}
    </select>
  );
}
