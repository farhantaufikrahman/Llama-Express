import { useState, type ChangeEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/TextField";
import { isApiError } from "../../../lib/api/errors";
import { formatRupiah } from "../../../lib/format";
import { compressImage } from "../../../lib/image";
import { useTopic } from "../../../lib/useRealtime";
import { useWakeLock } from "../../../lib/useWakeLock";
import { PickMap, type MapMarker } from "../../map/PickMap";
import { courierApi, courierKeys } from "../api";
import { useCourierRuntime } from "../courierRuntimeContext";
import type { CourierJob } from "../types";

const STEPS = ["Ke jemput", "Ambil paket", "Ke tujuan", "Selesai"];
const stepOf = (status: CourierJob["status"]) =>
  (
    ({
      ASSIGNED: 0,
      PICKING_UP: 0,
      PICKED_UP: 1,
      IN_TRANSIT: 2,
      DELIVERED: 3,
    }) as Record<string, number>
  )[status] ?? 0;

const mapsLink = (lat: number, lng: number) =>
  `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;

export default function CourierJobPage() {
  const queryClient = useQueryClient();
  const { location } = useCourierRuntime();
  const [error, setError] = useState<string | null>(null);
  const [delivered, setDelivered] = useState<CourierJob | null>(null);

  const { data: job, isPending } = useQuery({
    queryKey: courierKeys.activeJob,
    queryFn: ({ signal }) => courierApi.activeJob(signal),
  });

  useWakeLock(!!job); // layar tetap menyala selama mengantar

  // Customer/admin membatalkan saat kurir di jalan → kurir harus tahu SEKETIKA
  useTopic(job ? `/topic/orders/${job.code}/status` : null, () => {
    void queryClient.invalidateQueries({ queryKey: courierKeys.activeJob });
  });

  const onError = (e: unknown) =>
    setError(isApiError(e) ? e.message : "Terjadi kesalahan, coba lagi");
  const advance = useMutation({
    mutationFn: (action: "start" | "pickup") =>
      action === "start"
        ? courierApi.startPickup(job!.code)
        : courierApi.pickUp(job!.code),
    onSuccess: (updated) => {
      setError(null);
      queryClient.setQueryData(courierKeys.activeJob, updated);
    },
    onError,
  });

  if (delivered) return <DeliveredScreen job={delivered} />;
  if (isPending) return <p className="text-ink-muted">Memuat…</p>;
  if (!job) {
    return (
      <div className="flex max-w-xl flex-col gap-3">
        <Alert kind="info">
          Tidak ada tugas aktif. Tugas mungkin sudah selesai atau dibatalkan.
        </Alert>
        <Link
          to="/courier"
          className="font-bold text-brand-blue hover:underline"
        >
          ← Kembali ke beranda
        </Link>
      </div>
    );
  }

  const step = stepOf(job.status);
  const target = step === 0 ? job.pickup : job.dropoff;
  const markers: MapMarker[] = [
    {
      id: "pickup",
      kind: "pickup",
      position: job.pickup,
      title: "Titik jemput",
    },
    {
      id: "dropoff",
      kind: "dropoff",
      position: job.dropoff,
      title: "Titik antar",
    },
    ...(location.position
      ? [
          {
            id: "me",
            kind: "courier" as const,
            position: location.position,
            title: "Posisi saya",
          },
        ]
      : []),
  ];

  return (
    <section className="flex max-w-xl flex-col gap-4">
      <header className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-extrabold">{job.code}</h1>
          <span className="rounded-full bg-[#E8EFFE] px-3 py-1.5 text-xs font-bold text-[#1D4ED8]">
            Langkah {step + 1} dari 4
          </span>
        </div>
        <div className="grid grid-cols-4 gap-1.5" aria-hidden="true">
          {STEPS.map((_, i) => (
            <div
              key={i}
              className={`h-1.5 rounded-full ${i < step ? "bg-success" : i === step ? "bg-brand-blue" : "bg-line"}`}
            />
          ))}
        </div>
        <ol className="grid grid-cols-4 gap-1.5 text-[11px] font-semibold text-ink-muted">
          {STEPS.map((s, i) => (
            <li
              key={s}
              aria-current={i === step ? "step" : undefined}
              className={i === step ? "font-extrabold text-[#1D4ED8]" : ""}
            >
              {s}
            </li>
          ))}
        </ol>
      </header>

      {error && <Alert>{error}</Alert>}

      <PickMap markers={markers} showRoute className="h-56" />

      <div className="flex flex-col gap-3 rounded-2xl bg-white p-5">
        <p className="text-xs font-bold uppercase tracking-wider text-ink-muted">
          {step === 0 ? "Jemput di" : "Antar ke"}
        </p>
        <p className="text-lg font-extrabold">{target.address}</p>
        {step > 0 && (
          <p className="text-sm text-ink-muted">
            {job.recipientName} · {job.recipientPhone}
            {job.notes ? ` · “${job.notes}”` : ""}
          </p>
        )}
        <div className="grid grid-cols-2 gap-2.5">
          <a
            href={mapsLink(target.lat, target.lng)}
            target="_blank"
            rel="noreferrer"
            className="flex h-12 items-center justify-center rounded-xl bg-brand-blue text-[15px] font-bold text-white"
          >
            Navigasi
          </a>
          {step > 0 ? (
            <a
              href={`tel:${job.recipientPhone}`}
              className="flex h-12 items-center justify-center rounded-xl border-[1.5px] border-line text-[15px] font-bold"
            >
              Hubungi
            </a>
          ) : (
            <span className="flex h-12 items-center justify-center rounded-xl bg-cream text-sm font-bold">
              {job.itemDesc ?? job.itemCategory}
              {job.weightKg ? ` · ${job.weightKg} kg` : ""}
            </span>
          )}
        </div>
        {job.paymentMethod === "CASH" && (
          <Alert kind="info">
            Order tunai: tagih <b>{formatRupiah(job.priceTotal)}</b> saat serah
            terima.
          </Alert>
        )}
      </div>

      {job.status === "ASSIGNED" && (
        <Button
          onClick={() => advance.mutate("start")}
          disabled={advance.isPending}
          className="h-14 w-full"
        >
          Berangkat ke titik jemput
        </Button>
      )}
      {job.status === "PICKING_UP" && (
        <Button
          onClick={() => advance.mutate("pickup")}
          disabled={advance.isPending}
          className="h-14 w-full"
        >
          Paket sudah saya ambil
        </Button>
      )}
      {(job.status === "PICKED_UP" || job.status === "IN_TRANSIT") && (
        <CompleteDelivery job={job} onDone={setDelivered} onError={onError} />
      )}
    </section>
  );
}

function CompleteDelivery({
  job,
  onDone,
  onError,
}: {
  job: CourierJob;
  onDone: (job: CourierJob) => void;
  onError: (e: unknown) => void;
}) {
  const queryClient = useQueryClient();
  const [preview, setPreview] = useState<string | null>(null);
  const [photo, setPhoto] = useState<Blob | null>(null);
  const [receivedBy, setReceivedBy] = useState("");
  const [code, setCode] = useState("");
  const [proofUploaded, setProofUploaded] = useState(false);

  const onPhoto = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const compressed = await compressImage(file);
      setPhoto(compressed);
      setPreview((old) => {
        if (old) URL.revokeObjectURL(old); // bebaskan memori preview lama
        return URL.createObjectURL(compressed);
      });
      setProofUploaded(false);
    } catch (err) {
      onError(err);
    }
  };

  const upload = useMutation({
    mutationFn: () =>
      courierApi.uploadProof(job.code, photo!, receivedBy.trim() || undefined),
    onSuccess: () => setProofUploaded(true),
    onError,
  });

  const complete = useMutation({
    mutationFn: () => courierApi.complete(job.code, code),
    onSuccess: (done) => {
      queryClient.setQueryData(courierKeys.activeJob, null);
      void queryClient.invalidateQueries({ queryKey: ["courier"] });
      onDone(done);
    },
    onError,
  });

  return (
    <div className="flex flex-col gap-3 rounded-2xl bg-white p-5">
      <p className="text-sm font-bold">Bukti pengiriman</p>
      <label className="flex h-32 cursor-pointer flex-col items-center justify-center gap-2 overflow-hidden rounded-2xl border-2 border-dashed border-[#B8C0CE] bg-cream text-sm font-bold">
        {preview ? (
          <img
            src={preview}
            alt="Pratinjau foto bukti"
            className="size-full object-cover"
          />
        ) : (
          <>
            📷<span>Ambil foto paket diterima</span>
          </>
        )}
        {/* capture="environment" → di HP langsung membuka KAMERA BELAKANG, bukan galeri */}
        <input
          type="file"
          accept="image/*"
          capture="environment"
          className="sr-only"
          onChange={(e) => void onPhoto(e)}
        />
      </label>
      <TextField
        label="Diterima oleh (opsional)"
        value={receivedBy}
        onChange={(e) => setReceivedBy(e.target.value)}
      />
      <Button
        variant="secondary"
        onClick={() => upload.mutate()}
        disabled={!photo || upload.isPending || proofUploaded}
      >
        {proofUploaded
          ? "Foto terunggah ✓"
          : upload.isPending
            ? "Mengunggah…"
            : "Unggah foto"}
      </Button>

      <TextField
        label="Kode verifikasi dari penerima"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={4}
        placeholder="4 digit"
        className="text-center font-mono text-xl tracking-[0.5em]"
        value={code}
        onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 4))}
      />
      <Button
        onClick={() => complete.mutate()}
        disabled={!proofUploaded || code.length !== 4 || complete.isPending}
        className="h-14 w-full !bg-success !text-white"
      >
        {complete.isPending ? "Menyelesaikan…" : "Selesaikan pengantaran"}
      </Button>
      {!proofUploaded && (
        <p className="text-xs text-ink-muted">
          Unggah foto bukti dulu sebelum menyelesaikan.
        </p>
      )}
    </div>
  );
}

function DeliveredScreen({ job }: { job: CourierJob }) {
  return (
    <section className="flex max-w-xl flex-col items-center gap-4 rounded-3xl bg-white p-8 text-center">
      <div className="grid size-16 place-items-center rounded-full bg-success text-3xl text-white">
        ✓
      </div>
      <h1 className="text-2xl font-extrabold">Pengantaran selesai!</h1>
      <p className="text-ink-muted">
        {job.code} · {formatRupiah(job.priceTotal)}
      </p>
      <div className="flex gap-2">
        <Link
          to="/courier"
          className="rounded-xl bg-navy-900 px-5 py-3 text-sm font-bold text-gold-400"
        >
          Cari order lagi
        </Link>
        <Link
          to="/courier/earnings"
          className="rounded-xl border-[1.5px] border-line px-5 py-3 text-sm font-bold"
        >
          Lihat penghasilan
        </Link>
      </div>
    </section>
  );
}
