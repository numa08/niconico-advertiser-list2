// エラー表示（仕様: FRONTEND_REQUIREMENTS.md FE-145）
export function ErrorBox({ message }: { message: string }) {
  return (
    <div className="error-box">
      <h2>エラー</h2>
      <p>{message}</p>
    </div>
  );
}
