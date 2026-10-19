import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router";
import { Logo } from "../../../components/Logo";
import { Alert } from "../../../components/Alert";
import { isApiError } from "../../../lib/api/errors";
import { formatDateTime } from "../../../lib/format";
import { PickMap, type MapMarker } from "../../map/PickMap";
import { StatusBadge } from "../../orders/components/StatusBadge";
import { ACTIVE_STATUSES, ORDER_STATUS } from "../../orders/status";
import { trackingApi, trackingKeys } from "../api";
import { CourierCard } from "../components/CourierCard";

/** Untuk PENERIMA paket — tanpa login. Data dari backend sudah disaring (nama depan kurir, tanpa nomor HP). */
export default function PublicTrackingPage() {
  const { token = "" } = useParams();
  const { data, isPending, isError, error } = useQuery({
    queryKey: trackingKeys.public(token),
    queryFn: ({ signal }) => trackingApi.forPublic(token, signal),
    refetchInterval: (q) =>
      q.state.data && ACTIVE_STATUSES.has(q.state.data.status) ? 10_000 : false,
    retry: false,
  });

  return (
    <div className="min-h-dvh">
      <header className="border-b border-line bg-white">
        <div className="mx-auto flex h-16 max-w-3xl items-center px-4">
          <Logo size={32} />
        </div>
      </header>
      <main className="mx-auto flex max-w-3xl flex-col gap-4 px-4 py-6">
        {isPending && <p className="text-ink-muted">Memuat…</p>}
        {isError && (
          <Alert>
            {isApiError(error) && error.status === 404
              ? "Link lacak tidak valid atau sudah tidak berlaku."
              : error.message}
          </Alert>
        )}
        {data && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="text-xs font-semibold text-ink-muted">
                  Paket untukmu
                </p>
                <h1 className="text-2xl font-extrabold tracking-tight">
                  {data.orderCode}
                </h1>
              </div>
              <StatusBadge status={data.status} />
            </div>
            <PickMap
              showRoute
              className="h-80"
              path={data.route}
              markers={[
                {
                  id: "pickup",
                  kind: "pickup",
                  position: data.pickup,
                  title: "Titik jemput",
                },
                {
                  id: "dropoff",
                  kind: "dropoff",
                  position: data.dropoff,
                  title: "Tujuan",
                },
                ...((data.lastLocation && ACTIVE_STATUSES.has(data.status)
                  ? [
                      {
                        id: "courier",
                        kind: "courier",
                        position: data.lastLocation,
                        title: "Posisi kurir",
                      },
                    ]
                  : []) as MapMarker[]),
              ]}
            />
            {data.courier && (
              <CourierCard
                courier={data.courier}
                lastLocation={data.lastLocation}
              />
            )}
            <section className="rounded-2xl bg-white p-5">
              <h2 className="mb-3 text-lg font-extrabold">Perjalanan paket</h2>
              <ol className="flex flex-col gap-3">
                {[...data.timeline].reverse().map((t) => (
                  <li key={`${t.status}-${t.at}`} className="text-sm">
                    <span className="font-bold">
                      {ORDER_STATUS[t.status].label}
                    </span>
                    <span className="text-ink-muted">
                      {" "}
                      · {formatDateTime(t.at)}
                    </span>
                  </li>
                ))}
              </ol>
            </section>
            <p className="text-center text-xs text-ink-muted">
              Halaman ini diperbarui otomatis setiap 10 detik.
            </p>
          </>
        )}
      </main>
    </div>
  );
}
