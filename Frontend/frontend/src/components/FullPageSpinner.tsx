export function FullPageSpinner() {
  return (
    <div className="grid min-h-dvh place-items-center" role="status" aria-label="Memuat">
      <div className="size-10 animate-spin rounded-full border-4 border-line border-t-navy-900" />
    </div>
  );
}