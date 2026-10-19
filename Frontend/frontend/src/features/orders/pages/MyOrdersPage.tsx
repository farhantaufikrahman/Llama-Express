import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { formatDateTime, formatRupiah } from "../../../lib/format";
import { orderApi, orderKeys } from "../api";
import { StatusBadge } from "../components/StatusBadge";

export default function MyOrdersPage() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: orderKeys.list(),
    queryFn: ({ signal }) => orderApi.list(signal),
  });

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-extrabold tracking-tight">Order saya</h1>
        <Link
          to="/orders/new"
          className="rounded-xl bg-navy-900 px-4 py-2.5 text-sm font-bold text-gold-400 hover:bg-navy-800"
        >
          + Kirim paket
        </Link>
      </div>
      {isPending && <p className="text-ink-muted">Memuat…</p>}
      {isError && (
        <p role="alert" className="text-danger">
          {error.message}
        </p>
      )}
      {data?.content.length === 0 && (
        <p className="text-ink-muted">
          Belum ada order. Yuk kirim paket pertamamu!
        </p>
      )}
      <ul className="flex flex-col gap-3">
        {data?.content.map((o) => (
          <li key={o.code}>
            <Link
              to={`/orders/${o.code}`}
              className="block rounded-2xl bg-white p-4 transition hover:ring-2 hover:ring-line"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-bold">{o.code}</span>
                <StatusBadge status={o.status} />
              </div>
              <p className="mt-1 truncate text-sm text-ink-muted">
                {o.dropoffAddress}
              </p>
              <p className="mt-2 text-sm font-semibold">
                {formatRupiah(o.priceTotal)} · {formatDateTime(o.createdAt)}
              </p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
