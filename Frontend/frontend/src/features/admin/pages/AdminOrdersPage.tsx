import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { Pagination } from "../../../components/Pagination";
import { isApiError } from "../../../lib/api/errors";
import { saveBlob } from "../../../lib/download";
import { formatDateTime, formatRupiah } from "../../../lib/format";
import { StatusBadge } from "../../orders/components/StatusBadge";
import { ORDER_STATUS } from "../../orders/status";
import type {
  OrderStatus,
  PaymentMethod,
  ServiceType,
} from "../../orders/types";
import { adminApi, adminKeys } from "../api";
import type { OrderFilter } from "../types";

const selectClass =
  "h-11 rounded-xl border-[1.5px] border-line bg-white px-3 text-sm";

export default function AdminOrdersPage() {
  const [params, setParams] = useSearchParams();
  // URL = sumber kebenaran filter → bisa di-bookmark, dibagikan, dan tombol Back bekerja
  const filter: OrderFilter = {
    q: params.get("q") ?? undefined,
    status: (params.get("status") as OrderStatus) ?? undefined,
    serviceType: (params.get("serviceType") as ServiceType) ?? undefined,
    paymentMethod: (params.get("paymentMethod") as PaymentMethod) ?? undefined,
    from: params.get("from") ?? undefined,
    to: params.get("to") ?? undefined,
  };
  const page = Number(params.get("page") ?? 0);
  const [search, setSearch] = useState(filter.q ?? "");
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const update = (patch: Record<string, string | number | undefined>) =>
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      Object.entries(patch).forEach(([k, v]) =>
        v === undefined || v === "" ? next.delete(k) : next.set(k, String(v)),
      );
      if (!("page" in patch)) next.delete("page"); // ganti filter → kembali ke halaman pertama
      return next;
    });

  const { data, isPending, isError, error, isFetching } = useQuery({
    queryKey: adminKeys.orders(filter, page),
    queryFn: ({ signal }) => adminApi.orders(filter, page, signal),
    placeholderData: keepPreviousData, // pindah halaman tanpa tabel "berkedip" kosong
  });

  const exportCsv = async () => {
    setExporting(true);
    setExportError(null);
    try {
      const { blob, filename } = await adminApi.exportOrders(filter);
      saveBlob(blob, filename ?? "orders.csv");
    } catch (e) {
      setExportError(isApiError(e) ? e.message : "Ekspor gagal");
    } finally {
      setExporting(false);
    }
  };

  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-[26px] font-extrabold tracking-tight">
        Kelola order
      </h1>

      <form
        className="flex flex-wrap items-end gap-2.5 rounded-2xl bg-white p-4"
        onSubmit={(e) => {
          e.preventDefault();
          update({ q: search.trim() });
        }}
      >
        <label className="flex min-w-[220px] flex-[2] flex-col gap-1.5 text-xs font-semibold text-ink-muted">
          Cari (kode order, email, atau no. HP — persis)
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="LX-… / email / 0812…"
            className="h-11 rounded-xl border-[1.5px] border-line px-3 text-sm font-normal text-ink"
          />
        </label>
        <label className="flex flex-1 flex-col gap-1.5 text-xs font-semibold text-ink-muted">
          Status
          <select
            className={selectClass}
            value={filter.status ?? ""}
            onChange={(e) => update({ status: e.target.value })}
          >
            <option value="">Semua status</option>
            {(Object.keys(ORDER_STATUS) as OrderStatus[]).map((s) => (
              <option key={s} value={s}>
                {ORDER_STATUS[s].label}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-1 flex-col gap-1.5 text-xs font-semibold text-ink-muted">
          Layanan
          <select
            className={selectClass}
            value={filter.serviceType ?? ""}
            onChange={(e) => update({ serviceType: e.target.value })}
          >
            <option value="">Semua</option>
            <option value="INSTANT">Instant</option>
            <option value="SAME_DAY">Same Day</option>
          </select>
        </label>
        <label className="flex flex-1 flex-col gap-1.5 text-xs font-semibold text-ink-muted">
          Pembayaran
          <select
            className={selectClass}
            value={filter.paymentMethod ?? ""}
            onChange={(e) => update({ paymentMethod: e.target.value })}
          >
            <option value="">Semua</option>
            <option value="CASH">Tunai</option>
            <option value="MIDTRANS">Online</option>
          </select>
        </label>
        <label className="flex flex-col gap-1.5 text-xs font-semibold text-ink-muted">
          Dari
          <input
            type="date"
            className={selectClass}
            value={filter.from ?? ""}
            onChange={(e) => update({ from: e.target.value })}
          />
        </label>
        <label className="flex flex-col gap-1.5 text-xs font-semibold text-ink-muted">
          Sampai
          <input
            type="date"
            className={selectClass}
            value={filter.to ?? ""}
            min={filter.from}
            onChange={(e) => update({ to: e.target.value })}
          />
        </label>
        <Button type="submit">Terapkan</Button>
        <Button
          type="button"
          variant="secondary"
          disabled={exporting}
          onClick={() => void exportCsv()}
        >
          {exporting ? "Mengekspor…" : "Ekspor CSV"}
        </Button>
        {params.toString() && (
          <button
            type="button"
            className="h-11 px-2 text-sm font-bold text-brand-blue hover:underline"
            onClick={() => {
              setSearch("");
              setParams({});
            }}
          >
            Reset
          </button>
        )}
      </form>

      {exportError && <Alert>{exportError}</Alert>}
      {isError && <Alert>{error.message}</Alert>}

      <div
        className={`overflow-x-auto rounded-2xl bg-white p-4 transition-opacity ${isFetching ? "opacity-70" : ""}`}
      >
        <table className="w-full min-w-[900px] text-sm">
          <thead>
            <tr className="text-left text-xs uppercase tracking-wider text-ink-muted">
              {[
                "Kode",
                "Customer",
                "Kurir",
                "Layanan",
                "Total",
                "Bayar",
                "Status",
                "Dibuat",
              ].map((h) => (
                <th key={h} className="px-2 py-2.5">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isPending && (
              <tr>
                <td colSpan={8} className="p-4 text-ink-muted">
                  Memuat…
                </td>
              </tr>
            )}
            {data?.content.length === 0 && (
              <tr>
                <td colSpan={8} className="p-4 text-ink-muted">
                  Tidak ada order yang cocok dengan filter.
                </td>
              </tr>
            )}
            {data?.content.map((o) => (
              <tr
                key={o.code}
                className="border-t border-line hover:bg-cream/60"
              >
                <td className="px-2 py-3 font-bold">
                  <Link
                    to={`/admin/orders/${o.code}`}
                    className="text-brand-blue hover:underline"
                  >
                    {o.code}
                  </Link>
                </td>
                <td className="px-2 py-3">{o.customerName}</td>
                <td className="px-2 py-3">{o.courierName ?? "—"}</td>
                <td className="px-2 py-3">
                  {o.serviceType === "INSTANT" ? "Instant" : "Same Day"}
                </td>
                <td className="px-2 py-3">{formatRupiah(o.priceTotal)}</td>
                <td className="px-2 py-3">
                  {o.paymentMethod === "CASH" ? "Tunai" : "Online"} ·{" "}
                  {o.paymentStatus}
                </td>
                <td className="px-2 py-3">
                  <StatusBadge status={o.status} />
                </td>
                <td className="px-2 py-3 text-ink-muted">
                  {formatDateTime(o.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {data && (
          <Pagination
            page={data.page}
            size={data.size}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onChange={(p) => update({ page: p })}
          />
        )}
      </div>
    </section>
  );
}
