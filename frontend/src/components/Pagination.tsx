// ページネーション（仕様: FRONTEND_REQUIREMENTS.md FE-038, FE-039）
interface PaginationProps {
  page: number;
  hasNext: boolean;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, hasNext, onPageChange }: PaginationProps) {
  return (
    <nav className="pagination" aria-label="ページ切り替え">
      <button
        type="button"
        className="button button--secondary"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        ← 前へ
      </button>
      <span className="pagination__page">ページ {page}</span>
      <button
        type="button"
        className="button button--secondary"
        disabled={!hasNext}
        onClick={() => onPageChange(page + 1)}
      >
        次へ →
      </button>
    </nav>
  );
}
