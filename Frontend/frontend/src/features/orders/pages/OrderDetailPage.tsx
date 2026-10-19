import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useLocation, useNavigate, useParams } from "react-router";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { isApiError } from "../../../lib/api/errors";
import { formatDateTime, formatRupiah } from "../../../lib/format";
import { useRealtimeStatus } from "../../../lib/useRealtime";
import { PickMap, type MapMarker } from "../../map/PickMap";
import { PaymentPanel } from "../../payment/PaymentPanel";
import { COURIER_PHASE, trackingApi, trackingKeys } from "../../tracking/api";
import { CourierCard } from "../../tracking/components/CourierCard";
import { LiveIndicator } from "../../tracking/components/LiveIndicator";
import { useLiveOrder } from "../../tracking/useLiveOrder";
import { orderApi, orderKeys } from "../api";
import { StatusBadge } from "../components/StatusBadge";
import { ACTIVE_STATUSES, CANCELLABLE_STATUSES, ORDER_STATUS } from "../status";
import type { Order } from "../types";

export default function OrderDetailPage() {
  const { code = "" } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const realtime = useRealtimeStatus();
  const autoPay =
    (location.state as { autoPay?: boolean } | null)?.autoPay === true;

  // History state ikut tersimpan saat reload → hapus supaya popup bayar tidak terbuka lagi setiap reload
  useEffect(() => {
    if (autoPay) navigate(location.pathname, { replace: true, state: null });
  }, [autoPay, location.pathname, navigate]);

  const {
    data: order,
    isPending,
    isError,
    error,
  } = useQuery({
    queryKey: orderKeys.detail(code),
    queryFn: ({ signal }) => orderApi.detail(code, signal),
    // WebSocket tersambung → tidak perlu polling. Terputus → polling sebagai cadangan (graceful degradation)
    refetchInterval: (q) =>
      q.state.data &&
      ACTIVE_STATUSES.has(q.state.data.status) &&
      realtime !== "connected"
        ? 15_000
        : false,
  });

  const isActive = !!order && ACTIVE_STATUSES.has(order.status);
  useLiveOrder(isActive ? code : null); // berlangganan hanya selama order masih berjalan

  const { data: tracking } = useQuery({
    queryKey: trackingKeys.order(code),
    queryFn: ({ signal }) => trackingApi.forOrder(code, signal),
    enabled: !!order && COURIER_PHASE.has(order.status),
  });

  if (isPending) return <p className="text-ink-muted">Memuat…</p>;
  if (isError) {
    return isApiError(error) && error.status === 404 ? (
      <Alert>Order tidak ditemukan.</Alert>
    ) : (
      <Alert>{error.message}</Alert>
    );
  }

  const markers: MapMarker[] = [
    {
      id: "pickup",
      kind: "pickup",
      position: order.pickup,
      title: "Titik jemput",
    },
    {
      id: "dropoff",
      kind: "dropoff",
      position: order.dropoff,
      title: "Titik antar",
    },
    ...(tracking?.lastLocation && isActive
      ? [
          {
            id: "courier",
            kind: "courier" as const,
            position: tracking.lastLocation,
            title: "Posisi kurir",
          },
        ]
      : []),
  ];

  return (
    <article className="flex flex-col gap-4">
      <Link
        to="/orders"
        className="text-sm font-bold text-brand-blue hover:underline"
      >
        ← Order saya
      </Link>
      <header className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-extrabold tracking-tight">{order.code}</h1>
        <div className="flex items-center gap-2">
          {isActive && <LiveIndicator />}
          <StatusBadge status={order.status} />
        </div>
      </header>

      {order.status === "CREATED" &&
        order.paymentMethod === "MIDTRANS" &&
        order.paymentStatus !== "PAID" && (
          <PaymentPanel order={order} autoOpen={autoPay} />
        )}

      <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <PickMap
          showRoute
          markers={markers}
          path={tracking?.route ?? []}
          className="h-72 lg:h-full lg:min-h-96"
        />
        <div className="flex flex-col gap-3">
          {tracking?.courier && (
            <CourierCard
              courier={tracking.courier}
              lastLocation={isActive ? tracking.lastLocation : null}
            />
          )}
          {order.verificationCode && isActive && (
            <VerificationCard code={order.verificationCode} />
          )}
          <TrackingLinkCard token={order.trackingToken} />
          <Summary order={order} />
        </div>
      </div>

      <Timeline order={order} />
      {CANCELLABLE_STATUSES.has(order.status) && (
        <CancelSection code={order.code} />
      )}
    </article>
  );
}

function VerificationCard({ code }: { code: string }) {
  return (
    <div className="rounded-2xl bg-white p-4">
      <p className="text-xs font-semibold text-ink-muted">
        Kode serah terima — berikan ke PENERIMA, bukan ke kurir
      </p>
      <p className="mt-1 font-mono text-4xl font-extrabold tracking-[0.4em] text-navy-900">
        {code}
      </p>
    </div>
  );
}

function TrackingLinkCard({ token }: { token: string }) {
  const [copied, setCopied] = useState(false);
  const url = `${window.location.origin}/track/${token}`;
  const copy = async () => {
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl bg-white p-4">
      <div className="min-w-0">
        <p className="text-xs font-semibold text-ink-muted">
          Link lacak untuk penerima
        </p>
        <a
          href={url}
          target="_blank"
          rel="noreferrer"
          className="block truncate text-sm text-brand-blue hover:underline"
        >
          {url}
        </a>
      </div>
      <button
        onClick={() => void copy()}
        className="shrink-0 rounded-lg bg-cream px-3 py-2 text-sm font-bold"
      >
        {copied ? "Tersalin ✓" : "Salin"}
      </button>
    </div>
  );
}

function Summary({ order }: { order: Order }) {
  const rows: [string, string][] = [
    ["Jemput", order.pickup.address],
    [
      "Antar",
      `${order.dropoff.address} · ${order.recipientName} (${order.recipientPhone})`,
    ],
    [
      "Layanan",
      `${order.serviceType === "INSTANT" ? "Instant" : "Same Day"} · ${order.vehicleType === "MOTOR" ? "Motor" : "Mobil"}`,
    ],
    [
      "Jarak",
      `${(order.distanceMeters / 1000).toLocaleString("id-ID", { maximumFractionDigits: 1 })} km`,
    ],
    [
      "Pembayaran",
      `${order.paymentMethod === "CASH" ? "Tunai" : "Online"} · ${order.paymentStatus}`,
    ],
  ];
  return (
    <dl className="flex flex-col gap-2.5 rounded-2xl bg-white p-4 text-sm">
      {rows.map(([k, v]) => (
        <div key={k} className="grid grid-cols-[96px_1fr] gap-2">
          <dt className="font-semibold text-ink-muted">{k}</dt>
          <dd>{v}</dd>
        </div>
      ))}
      <div className="mt-1 flex items-center justify-between border-t border-line pt-3">
        <dt className="font-semibold text-ink-muted">Total</dt>
        <dd className="text-xl font-extrabold">
          {formatRupiah(order.priceTotal)}
        </dd>
      </div>
    </dl>
  );
}

function Timeline({ order }: { order: Order }) {
  return (
    <section className="rounded-2xl bg-white p-5">
      <h2 className="mb-3 text-lg font-extrabold">Riwayat status</h2>
      <ol className="relative flex flex-col gap-4 border-l-2 border-line pl-5">
        {[...order.timeline].reverse().map((t, i) => (
          <li key={`${t.status}-${t.at}`} className="relative">
            <span
              className={`absolute -left-[27px] top-1 size-3 rounded-full ${i === 0 ? "bg-navy-900" : "bg-line"}`}
            />
            <p className="text-sm font-bold">{ORDER_STATUS[t.status].label}</p>
            <p className="text-xs text-ink-muted">
              {formatDateTime(t.at)}
              {t.note ? ` · ${t.note}` : ""}
            </p>
          </li>
        ))}
      </ol>
    </section>
  );
}

function CancelSection({ code }: { code: string }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const cancel = useMutation({
    mutationFn: () => orderApi.cancel(code, reason.trim()),
    onSuccess: (updated) => {
      queryClient.setQueryData(orderKeys.detail(code), updated);
      void queryClient.invalidateQueries({ queryKey: orderKeys.list() });
      setOpen(false);
    },
  });

  if (!open) {
    return (
      <Button
        variant="secondary"
        className="self-start text-danger"
        onClick={() => setOpen(true)}
      >
        Batalkan order
      </Button>
    );
  }
  return (
    <section className="flex flex-col gap-3 rounded-2xl bg-white p-5">
      <label htmlFor="cancel-reason" className="text-sm font-bold">
        Alasan pembatalan
      </label>
      <input
        id="cancel-reason"
        value={reason}
        maxLength={255}
        onChange={(e) => setReason(e.target.value)}
        className="h-12 rounded-xl border-[1.5px] border-line px-3.5 outline-none focus:border-brand-blue"
      />
      {cancel.isError && <Alert>{cancel.error.message}</Alert>}
      <div className="flex gap-2">
        <Button
          variant="danger"
          disabled={!reason.trim() || cancel.isPending}
          onClick={() => cancel.mutate()}
        >
          {cancel.isPending ? "Membatalkan…" : "Ya, batalkan"}
        </Button>
        <Button variant="secondary" onClick={() => setOpen(false)}>
          Kembali
        </Button>
      </div>
    </section>
  );
}
