import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/client';
import { formatRupiah } from '../../../lib/format';

interface DashboardSummary {
  ordersToday: number;
  activeOrders: number;
  searchingCourier: number;
  couriersOnline: number;
  deliveredToday: number;
  revenueToday: number;
  avgDeliveryMinutes: number | null;
}

export default function AdminDashboardPage() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: ({ signal }) => api<DashboardSummary>('/api/v1/admin/dashboard/summary', { signal }),
    refetchInterval: 15_000, // dashboard operasional: segarkan otomatis tiap 15 detik
  });

  if (isPending) return <p className="text-ink-muted">Memuat…</p>;
  if (isError) return <p role="alert" className="text-danger">{error.message}</p>;

  const stats = [
    { label: 'Order hari ini', value: String(data.ordersToday) },
    { label: 'Order aktif', value: String(data.activeOrders) },
    { label: 'Mencari kurir', value: String(data.searchingCourier) },
    { label: 'Kurir online', value: String(data.couriersOnline) },
    { label: 'Pendapatan hari ini', value: formatRupiah(data.revenueToday) },
    { label: 'Rata-rata waktu antar', value: data.avgDeliveryMinutes == null ? '—' : `${Math.round(data.avgDeliveryMinutes)} mnt` },
  ];

  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-2xl font-extrabold tracking-tight">Operasional hari ini</h1>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-3.5">
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl bg-white p-4">
            <p className="text-[13px] font-semibold text-ink-muted">{s.label}</p>
            <p className="mt-1 text-[28px] font-extrabold tracking-tight">{s.value}</p>
          </div>
        ))}
      </div>
    </section>
  );
}