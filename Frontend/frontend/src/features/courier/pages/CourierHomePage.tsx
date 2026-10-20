import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { Alert } from "../../../components/Alert";
import { isApiError } from "../../../lib/api/errors";
import { courierApi, courierKeys } from "../api";
import { useCourierRuntime } from "../courierRuntimeContext";
import { LocationStatusBar } from "../components/LocationStatusBar";
import { OfferCard } from "../components/OfferCard";

export default function CourierHomePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { profile, location } = useCourierRuntime();
  const [error, setError] = useState<string | null>(null);

  const verified = profile?.verificationStatus === "VERIFIED";
  const online = profile?.online ?? false;

  const activeJob = useQuery({
    queryKey: courierKeys.activeJob,
    queryFn: ({ signal }) => courierApi.activeJob(signal),
    enabled: verified,
  });

  // Tawaran hanya relevan saat online & tidak sedang mengantar → polling 3 detik (TTL tawaran pendek)
  const offers = useQuery({
    queryKey: courierKeys.offers,
    queryFn: ({ signal }) => courierApi.offers(signal),
    enabled: verified && online && !activeJob.data,
    refetchInterval: 3_000,
  });

  const showError = (e: unknown) =>
    setError(isApiError(e) ? e.message : "Terjadi kesalahan, coba lagi");

  const toggleOnline = useMutation({
    mutationFn: (next: boolean) => courierApi.setOnline(next),
    onSuccess: (updated) => {
      setError(null);
      queryClient.setQueryData(courierKeys.me, updated); // backend mengembalikan profil terbaru → tak perlu fetch ulang
      void queryClient.invalidateQueries({ queryKey: courierKeys.offers });
    },
    onError: showError,
  });

  const respond = useMutation({
    // async + await di kedua cabang → tipe kembalian selalu Promise<void>
    mutationFn: async ({
      offerId,
      accept,
    }: {
      offerId: number;
      accept: boolean;
    }) => {
      if (accept) await courierApi.accept(offerId);
      else await courierApi.reject(offerId);
    },
    onSuccess: (_data, { accept }) => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey: courierKeys.offers });
      void queryClient.invalidateQueries({ queryKey: courierKeys.activeJob });
      if (accept) navigate("/courier/job");
    },
    onError: (e) => {
      showError(e);
      void queryClient.invalidateQueries({ queryKey: courierKeys.offers }); // tawaran mungkin sudah kedaluwarsa/diambil kurir lain
    },
  });

  if (!profile) return <p className="text-ink-muted">Memuat…</p>;

  if (!verified) {
    return (
      <section className="flex flex-col gap-4">
        <h1 className="text-2xl font-extrabold tracking-tight">
          Halo, {profile.fullName}
        </h1>
        <Alert kind="info">
          {profile.verificationStatus === "REJECTED"
            ? "Pendaftaran kamu ditolak. Hubungi admin untuk informasi lebih lanjut."
            : "Akun kamu sedang diverifikasi admin. Kamu bisa mulai narik setelah disetujui."}
        </Alert>
      </section>
    );
  }

  return (
    <section className="flex flex-col gap-4">
      <header className="flex items-center justify-between gap-3 rounded-2xl bg-navy-900 p-5 text-white">
        <div>
          <p className="text-sm text-white/70">Halo, {profile.fullName}</p>
          <p className="text-xl font-extrabold">
            {online ? "Kamu sedang online" : "Kamu sedang offline"}
          </p>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={online}
          aria-label="Status online"
          disabled={toggleOnline.isPending || Boolean(activeJob.data)}
          onClick={() => toggleOnline.mutate(!online)}
          className={`relative h-9 w-16 rounded-full transition-colors disabled:opacity-50 ${online ? "bg-gold-400" : "bg-white/25"}`}
        >
          <span
            className={`absolute top-1 h-7 w-7 rounded-full bg-white shadow transition-all ${online ? "left-8" : "left-1"}`}
          />
        </button>
      </header>

      {online && (
        <LocationStatusBar
          status={location.status}
          lastSentAt={location.lastSentAt}
        />
      )}
      {error && <Alert>{error}</Alert>}

      {activeJob.data ? (
        <Link to="/courier/job" className="block rounded-2xl bg-white p-5">
          <p className="text-sm font-semibold text-ink-muted">
            Pengantaran aktif
          </p>
          <p className="text-lg font-extrabold">{activeJob.data.code}</p>
          <p className="mt-1 text-sm font-bold text-brand-blue">
            Lanjutkan tugas →
          </p>
        </Link>
      ) : !online ? (
        <p className="rounded-2xl bg-white p-5 text-ink-muted">
          Aktifkan status online untuk mulai menerima tawaran order.
        </p>
      ) : offers.data && offers.data.length > 0 ? (
        <ul className="flex flex-col gap-3">
          {offers.data.map((offer) => (
            <li key={offer.offerId}>
              <OfferCard
                offer={offer}
                busy={respond.isPending}
                onAccept={() =>
                  respond.mutate({ offerId: offer.offerId, accept: true })
                }
                onReject={() =>
                  respond.mutate({ offerId: offer.offerId, accept: false })
                }
              />
            </li>
          ))}
        </ul>
      ) : (
        <p className="rounded-2xl bg-white p-5 text-ink-muted">
          Menunggu tawaran order di sekitarmu…
        </p>
      )}
    </section>
  );
}
