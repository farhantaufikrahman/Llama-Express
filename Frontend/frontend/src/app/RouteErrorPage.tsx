import { isRouteErrorResponse, Link, useRouteError } from 'react-router';

/** Padanan @ControllerAdvice untuk UI: menangkap error render/loader di seluruh route */
export function RouteErrorPage() {
  const error = useRouteError();
  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error ? error.message : 'Terjadi kesalahan';

  if (import.meta.env.DEV) console.error(error); // detail lengkap hanya di console saat development

  return (
    <main className="grid min-h-dvh place-items-center px-4 text-center">
      <div className="max-w-md">
        <p className="text-2xl font-extrabold text-navy-900">Ups, ada yang tidak beres</p>
        <p className="mt-2 text-sm text-ink-muted">Coba muat ulang halaman. Kalau masih terjadi, kembali ke beranda.</p>
        {import.meta.env.DEV && <pre className="mt-4 overflow-auto rounded-xl bg-white p-3 text-left text-xs text-danger">{message}</pre>}
        <div className="mt-6 flex justify-center gap-3">
          <button onClick={() => window.location.reload()} className="rounded-xl bg-navy-900 px-4 py-2.5 text-sm font-bold text-gold-400">Muat ulang</button>
          <Link to="/" className="rounded-xl border-[1.5px] border-line bg-white px-4 py-2.5 text-sm font-bold">Ke beranda</Link>
        </div>
      </div>
    </main>
  );
}