import { Link } from 'react-router';

export default function NotFoundPage() {
  return (
    <main className="grid min-h-dvh place-items-center px-4 text-center">
      <div>
        <p className="text-6xl font-extrabold text-navy-900">404</p>
        <p className="mt-2 text-ink-muted">Halaman tidak ditemukan.</p>
        <Link to="/" className="mt-6 inline-block font-bold text-brand-blue hover:underline">Kembali ke beranda</Link>
      </div>
    </main>
  );
}