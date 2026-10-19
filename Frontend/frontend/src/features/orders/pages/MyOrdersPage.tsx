import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api/client';
import type { PageResponse } from '../../lib/api/types';
import { formatDateTime, formatRupiah } from '../../lib/format';

interface OrderSummary {
  code: string;
  status: string;
  serviceType: 'INSTANT' | 'SAME_DAY';
  dropoffAddress: string;
  priceTotal: number;
  createdAt: string;
}

export default function MyOrdersPage() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['orders', 'mine'],
    queryFn: ({ signal }) => api<PageResponse<OrderSummary>>('/api/v1/orders?page=0&size=20', { signal }),
  });

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-extrabold tracking-tight">Order saya</h1>
      {isPending && <p className="text-ink-muted">Memuat…</p>}
      {isError && <p role="alert" className="text-danger">{error.message}</p>}
      {data?.content.length === 0 && <p className="text-ink-muted">Belum ada order.</p>}
      <ul className="flex flex-col gap-3">
        {data?.content.map((o) => (
          <li key={o.code} className="rounded-2xl bg-white p-4">
            <div className="flex items-center justify-between">
              <span className="font-bold">{o.code}</span>
              <span className="rounded-full bg-cream px-3 py-1 text-xs font-bold">{o.status}</span>
            </div>
            <p className="mt-1 text-sm text-ink-muted">{o.dropoffAddress}</p>
            <p className="mt-2 text-sm font-semibold">
              {formatRupiah(o.priceTotal)} · {formatDateTime(o.createdAt)}
            </p>
          </li>
        ))}
      </ul>
    </section>
  );
}