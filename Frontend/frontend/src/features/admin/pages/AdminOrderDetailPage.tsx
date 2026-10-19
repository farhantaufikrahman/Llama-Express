import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { ConfirmDialog } from "../../../components/ConfirmDialog";
import { isApiError } from "../../../lib/api/errors";
import { formatDateTime, formatRupiah } from "../../../lib/format";
import { PickMap } from "../../map/PickMap";
import { StatusBadge } from "../../orders/components/StatusBadge";
import { ACTIVE_STATUSES, ORDER_STATUS } from "../../orders/status";
import { adminApi, adminKeys } from "../api";
import type { Party } from "../types";

export default function AdminOrderDetailPage() {
  const { code = "" } = useParams();
  const queryClient = useQueryClient();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data, isPending, isError, error } = useQuery({
    queryKey: adminKeys.order(code),
    queryFn: ({ signal }) => adminApi.order(code, signal),
  });

  const cancel = useMutation({
    mutationFn: (reason: string) => adminApi.cancelOrder(code, reason),
    onSuccess: (updated) => {
      queryClient.setQueryData(adminKeys.order(code), updated);
      void queryClient.invalidateQueries({ queryKey: adminKeys.all });
      setConfirmOpen(false);
    },
  });

  if (isPending) return <p className="text-ink-muted">Memuat…</p>;
  if (isError)
    return (
      <Alert>
        {isApiError(error) && error.status === 404
          ? "Order tidak ditemukan."
          : error.message}
      </Alert>
    );

  const { order, customer, courier, latestPayment } = data;
  const paidOnline =
    order.paymentMethod === "MIDTRANS" && order.paymentStatus === "PAID";

  return (
    <article className="flex flex-col gap-4">
      <Link
        to="/admin/orders"
        className="text-sm font-bold text-brand-blue hover:underline"
      >
        ← Kelola order
      </Link>
      <header className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-extrabold tracking-tight">{order.code}</h1>
        <div className="flex items-center gap-2">
          <StatusBadge status={order.status} />
          {ACTIVE_STATUSES.has(order.status) && (
            <Button
              variant="danger"
              className="h-10"
              onClick={() => setConfirmOpen(true)}
            >
              Batalkan
            </Button>
          )}
        </div>
      </header>

      <div className="grid gap-4 lg:grid-cols-[1fr_380px]">
        <PickMap
          showRoute
          className="h-72 lg:h-full lg:min-h-80"
          markers={[
            {
              id: "pickup",
              kind: "pickup",
              position: order.pickup,
              title: "Jemput",
            },
            {
              id: "dropoff",
              kind: "dropoff",
              position: order.dropoff,
              title: "Antar",
            },
          ]}
        />
        <div className="flex flex-col gap-3">
          <PartyCard title="Customer" party={customer} />
          <PartyCard title="Kurir" party={courier} />
          <dl className="grid grid-cols-[110px_1fr] gap-2 rounded-2xl bg-white p-4 text-sm">
            <dt className="font-semibold text-ink-muted">Total</dt>
            <dd className="font-extrabold">{formatRupiah(order.priceTotal)}</dd>
            <dt className="font-semibold text-ink-muted">Pembayaran</dt>
            <dd>
              {order.paymentMethod} · {order.paymentStatus}
            </dd>
            {latestPayment && (
              <>
                <dt className="font-semibold text-ink-muted">Transaksi</dt>
                <dd className="break-all">
                  {latestPayment.merchantOrderId} · {latestPayment.status}
                </dd>
              </>
            )}
            <dt className="font-semibold text-ink-muted">Dibuat</dt>
            <dd>{formatDateTime(order.createdAt)}</dd>
            {order.cancelReason && (
              <>
                <dt className="font-semibold text-ink-muted">Alasan batal</dt>
                <dd>{order.cancelReason}</dd>
              </>
            )}
          </dl>
        </div>
      </div>

      <section className="rounded-2xl bg-white p-5">
        <h2 className="mb-3 text-lg font-extrabold">Riwayat status</h2>
        <ol className="flex flex-col gap-2.5">
          {order.timeline.map((t) => (
            <li key={`${t.status}-${t.at}`} className="text-sm">
              <span className="font-bold">{ORDER_STATUS[t.status].label}</span>
              <span className="text-ink-muted">
                {" "}
                · {formatDateTime(t.at)}
                {t.note ? ` · ${t.note}` : ""}
              </span>
            </li>
          ))}
        </ol>
      </section>

      <ConfirmDialog
        open={confirmOpen}
        danger
        withReason
        title={`Batalkan ${order.code}?`}
        description={
          paidOnline
            ? "Order ini SUDAH DIBAYAR online. Pembatalan tidak otomatis mengembalikan dana — lakukan refund manual di dashboard Midtrans."
            : "Customer dan kurir akan melihat order ini dibatalkan."
        }
        confirmLabel="Ya, batalkan"
        busy={cancel.isPending}
        error={cancel.isError ? cancel.error.message : null}
        onConfirm={(reason) => cancel.mutate(reason)}
        onClose={() => setConfirmOpen(false)}
      />
    </article>
  );
}

function PartyCard({ title, party }: { title: string; party: Party | null }) {
  return (
    <div className="rounded-2xl bg-white p-4 text-sm">
      <p className="text-xs font-semibold text-ink-muted">{title}</p>
      {party ? (
        <>
          <p className="font-bold">{party.fullName}</p>
          <p className="text-ink-muted">
            {party.email} ·{" "}
            <a href={`tel:${party.phone}`} className="text-brand-blue">
              {party.phone}
            </a>
          </p>
        </>
      ) : (
        <p className="text-ink-muted">Belum ada</p>
      )}
    </div>
  );
}
