export function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onChange: (page: number) => void;
}) {
  if (totalElements === 0) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <nav
      className="flex items-center justify-between gap-3 pt-3 text-[13px] text-ink-muted"
      aria-label="Halaman"
    >
      <span>
        Menampilkan {from}–{to} dari {totalElements.toLocaleString("id-ID")}
      </span>
      <div className="flex gap-1.5">
        <button
          onClick={() => onChange(page - 1)}
          disabled={page === 0}
          className="h-9 rounded-lg border-[1.5px] border-line bg-white px-3 font-semibold text-navy-900 disabled:opacity-40"
        >
          Sebelumnya
        </button>
        <button
          onClick={() => onChange(page + 1)}
          disabled={page + 1 >= totalPages}
          className="h-9 rounded-lg border-[1.5px] border-line bg-white px-3 font-semibold text-navy-900 disabled:opacity-40"
        >
          Berikutnya
        </button>
      </div>
    </nav>
  );
}
