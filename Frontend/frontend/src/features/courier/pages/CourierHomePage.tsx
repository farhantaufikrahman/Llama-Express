import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { isApiError } from "../../../lib/api/errors";
import { ORDER_STATUS } from "../../orders/status";
import { courierApi, courierKeys } from "../api";
import { useCourierRuntime } from "../CourierRuntime";
import { LocationStatusBar } from "../components/LocationStatusBar";
import { OfferCard } from "../components/OfferCard";

export default function CourierHomePage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { profile, location } = useCourierRuntime();
  const [error, setError] = useState<string | null>(null);
  const online = profile?.online === true;

  const { data: job } = useQuery({
    queryKey: courierKeys.activeJob,
    queryFn: ({ signal }) => courierApi.activeJob(signal),
    enabled: !!profile,
  });

  // Tawaran tidak lewat WebSocket di backend kita → polling cepat, HANYA saat online & tidak sedang mengantar
  const { data: offers = [] } = useQuery({
    queryKey: courierKeys.offers,
    queryFn: ({ signal }) => courierApi.offers(signal),
    enabled: online && !job,
    refetchInterval: 3_000,
  });

  const toggle = useMutation({
    mutationFn: (next: boolean) => courierApi.setOnline(next),
    onSuccess: (updated) => {
      queryClient.setQueryData(courierKeys.me, updated);
      setError(null);
    },
    onError: (e) =>
      setError(isApiError(e) ? e.message : "Gagal mengubah status"),
  });

  const respond = useMutation({
    mutationFn: ({ offerId, accept }: { offerId: number; accept: boolean }) =>
      accept ? courierApi.accept(offerId) : courierApi.reject(offerId),
    onSuccess: async (result, { accept }) => {
      if (accept && result) {
        queryClient.setQueryData(courierKeys.activeJob, result);
        navigate("/courier/job");
      }
      await queryClient.invalidateQueries({ queryKey: courierKeys.offers });
    },
    onError: (e) => {
      // 409 (keduluan kurir lain / kedaluwarsa) adalah hal NORMAL di sistem dispatch, bukan bug
      setError(isApiError(e) ? e.message : "Gagal merespons tawaran");
      void queryClient.invalidateQueries({ queryKey: courierKeys.offers });
    },
  });

  if (!profile) return <p className="text-ink-muted">Memuat…</p>;

  return (
    <section className="flex max-w-xl flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white p-5">
        <div>
          <h1 className="text-xl font-extrabold">
            Halo, {profile.fullName.split(" ")[0]}
          </h1>
          <p className="text-sm text-ink-muted">
            {profile.vehicleType === "MOTOR" ? "Motor" : "Mobil"} ·{" "}
            {profile.plateNumber}
            {profile.ratingAvg != null &&
              ` · ★ ${Number(profile.ratingAvg).toFixed(1)} (${profile.ratingCount})`}
          </p>
        </div>
        <button
          role="switch"
          aria-checked={online}
          onClick={() => toggle.mutate(!online)}
          disabled={
            toggle.isPending || profile.verificationStatus !== "VERIFIED"
          }
          className={`flex h-12 items-center gap-2.5 rounded-full px-5 text-sm font-extrabold transition disabled:opacity-50
            ${online ? "bg-success text-white" : "bg-navy-900 text-gold-400"}`}
        >
          <span
            className={`size-2.5 rounded-full ${online ? "bg-white" : "bg-gold-400"}`}
          />
          {toggle.isPending ? "…" : online ? "Online" : "Mulai narik"}
        </button>
      </div>

      {profile.verificationStatus === "PENDING" && (
        <Alert kind="info">
          Akunmu sedang diverifikasi admin. Kamu bisa online setelah disetujui.
        </Alert>
      )}
      {profile.verificationStatus === "REJECTED" && (
        <Alert>Verifikasi akun ditolak. Hubungi admin.</Alert>
      )}
      {error && <Alert>{error}</Alert>}

      <div className="rounded-2xl bg-white px-5 py-3.5">
        <LocationStatusBar
          status={location.status}
          lastSentAt={location.lastSentAt}
        />
      </div>

      {job && (
        <Link
          to="/courier/job"
          className="flex items-center justify-between rounded-2xl bg-navy-900 p-5 text-cream"
        >
          <div>
            <p className="text-xs font-semibold text-[#C9D1E3]">
              Tugas aktif · {ORDER_STATUS[job.status].label}
            </p>
            <p className="text-lg font-extrabold text-gold-400">{job.code}</p>
          </div>
          <span className="text-sm font-bold">Lanjutkan →</span>
        </Link>
      )}

      {online && !job && offers.length === 0 && (
        <div className="rounded-2xl border-2 border-dashed border-line p-8 text-center text-sm text-ink-muted">
          Menunggu order di sekitarmu…
        </div>
      )}

      {!job &&
        offers.map((offer) => (
          <OfferCard
            key={offer.offerId}
            offer={offer}
            busy={respond.isPending}
            onAccept={() =>
              respond.mutate({ offerId: offer.offerId, accept: true })
            }
            onReject={() =>
              respond.mutate({ offerId: offer.offerId, accept: false })
            }
          />
        ))}
    </section>
  );
}
