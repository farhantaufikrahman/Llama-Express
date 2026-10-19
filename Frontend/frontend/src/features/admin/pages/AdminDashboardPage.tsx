import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { Alert } from "../../../components/Alert";
import { formatRupiah } from "../../../lib/format";
import { StatusBadge } from "../../orders/components/StatusBadge";
import type { OrderStatus } from "../../orders/types";
import { adminApi, adminKeys } from "../api";

export default function AdminDashboardPage() {
  const { data, isPending, isError, error, dataUpdatedAt } = useQuery({
    queryKey: adminKeys.dashboard,
    queryFn: ({ signal }) => adminApi.dashboard(signal),
    refetchInterval: 15_000,
  });
  const pending = useQuery({
    queryKey: adminKeys.couriers("PENDING", 0),
    queryFn: ({ signal }) => adminApi.couriers("PENDING", 0, signal),
  });

  if (isPending) return <p className="text-ink-muted">Memuat…</p>;
  if (isError) return <Alert>{error.message}</Alert>;

  const stats = [
    {
      label: "Order hari ini",
      value: data.ordersToday.toLocaleString("id-ID"),
    },
    { label: "Order aktif", value: data.activeOrders.toLocaleString("id-ID") },
    {
      label: "Mencari kurir",
      value: data.searchingCourier.toLocaleString("id-ID"),
      warn: data.searchingCourier > 0,
    },
    {
      label: "Kurir online",
      value: data.couriersOnline.toLocaleString("id-ID"),
    },
    {
      label: "Terkirim hari ini",
      value: data.deliveredToday.toLocaleString("id-ID"),
    },
    { label: "Pendapatan hari ini", value: formatRupiah(data.revenueToday) },
    {
      label: "Rata-rata waktu antar",
      value:
        data.avgDeliveryMinutes == null
          ? "—"
          : `${Math.round(data.avgDeliveryMinutes)} mnt`,
    },
  ];
  const breakdown = Object.entries(data.statusBreakdown) as [
    OrderStatus,
    number,
  ][];

  return (
    <section className="flex flex-col gap-5">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="text-[26px] font-extrabold tracking-tight">
            Operasional hari ini
          </h1>
          <p className="text-sm text-ink-muted">
            Diperbarui {new Date(dataUpdatedAt).toLocaleTimeString("id-ID")} ·
            otomatis tiap 15 detik
          </p>
        </div>
        <Link
          to="/admin/orders"
          className="text-sm font-bold text-brand-blue hover:underline"
        >
          Kelola order →
        </Link>
      </div>

      {(pending.data?.totalElements ?? 0) > 0 && (
        <Link to="/admin/couriers" className="block">
          <Alert kind="info">
            {pending.data!.totalElements} kurir menunggu verifikasi — tinjau
            sekarang →
          </Alert>
        </Link>
      )}

      <div className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-3.5">
        {stats.map((s) => (
          <div key={s.label} className="rounded-2xl bg-white p-4">
            <p className="text-[13px] font-semibold text-ink-muted">
              {s.label}
            </p>
            <p
              className={`mt-1 text-[28px] font-extrabold tracking-tight ${s.warn ? "text-[#7A5300]" : ""}`}
            >
              {s.value}
            </p>
          </div>
        ))}
      </div>

      <section className="rounded-2xl bg-white p-5">
        <h2 className="mb-3 text-base font-extrabold">
          Order hari ini per status
        </h2>
        {breakdown.length === 0 ? (
          <p className="text-sm text-ink-muted">Belum ada order hari ini.</p>
        ) : (
          <ul className="flex flex-col divide-y divide-line">
            {breakdown.map(([status, count]) => (
              <li
                key={status}
                className="flex items-center justify-between py-2.5"
              >
                <Link
                  to={`/admin/orders?status=${status}&from=${data.date}`}
                  className="hover:underline"
                >
                  <StatusBadge status={status} />
                </Link>
                <span className="font-extrabold">{count}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </section>
  );
}
