import type { UseQueryResult } from '@tanstack/react-query';
import { formatRupiah } from '../../../lib/format';
import type { Quote } from '../types';

export function QuoteCard({ query, ready }: { query: UseQueryResult<Quote>; ready: boolean }) {
  if (!ready) {
    return <div className="rounded-2xl bg-white p-4 text-sm text-ink-muted">Pilih titik jemput & antar untuk melihat harga.</div>;
  }
  if (query.isError) {
    return <div role="alert" className="rounded-2xl bg-danger/10 p-4 text-sm font-medium text-danger">{query.error.message}</div>;
  }
  const q = query.data;
  return (
    <div className={`rounded-2xl bg-navy-900 p-4 text-cream transition-opacity ${query.isFetching ? 'opacity-70' : ''}`} aria-live="polite">
      <p className="text-xs font-semibold text-[#C9D1E3]">Total ongkir</p>
      <p className="text-3xl font-extrabold tracking-tight text-gold-400">{q ? formatRupiah(q.total) : '…'}</p>
      {q && (
        <p className="mt-1 text-sm text-[#C9D1E3]">
          {(q.distanceMeters / 1000).toLocaleString('id-ID', { maximumFractionDigits: 1 })} km · ±{q.etaMinutes} menit ·
          dasar {formatRupiah(q.baseFare)} + jarak {formatRupiah(q.distanceFare)}
        </p>
      )}
    </div>
  );
}