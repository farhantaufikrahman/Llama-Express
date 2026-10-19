import { formatRupiah } from "../../../lib/format";
import { useNow } from "../../../lib/useNow";
import { Button } from "../../../components/Button";
import type { Offer } from "../types";

const CATEGORY = {
  DOCUMENT: "Dokumen",
  FOOD: "Makanan",
  GOODS: "Barang",
} as const;

export function OfferCard({
  offer,
  busy,
  onAccept,
  onReject,
}: {
  offer: Offer;
  busy: boolean;
  onAccept: () => void;
  onReject: () => void;
}) {
  const now = useNow();
  // Catatan: hitung mundur memakai jam HP. Kalau jam HP meleset, backend tetap hakimnya (409 saat accept).
  const secondsLeft = Math.max(
    0,
    Math.ceil((new Date(offer.expiresAt).getTime() - now) / 1000),
  );
  const expired = secondsLeft === 0;

  return (
    <article className="flex flex-col gap-4 rounded-3xl bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <span className="rounded-full bg-[#FFF4D6] px-3 py-1.5 text-xs font-extrabold text-[#7A5300]">
          Order baru ·{" "}
          {offer.serviceType === "INSTANT" ? "Instant" : "Same Day"}
        </span>
        <span
          className={`font-mono text-sm font-extrabold ${secondsLeft <= 10 ? "text-danger" : "text-navy-900"}`}
          aria-live="polite"
        >
          00:{String(secondsLeft).padStart(2, "0")}
        </span>
      </div>

      <div>
        <p className="text-xs font-semibold text-ink-muted">Ongkir order</p>
        <p className="text-3xl font-extrabold tracking-tight">
          {formatRupiah(offer.priceTotal)}
        </p>
      </div>

      <div className="flex gap-3.5">
        <div className="flex flex-col items-center pt-1.5">
          <span className="size-3 rounded-full bg-brand-blue" />
          <span className="h-10 w-0.5 bg-line" />
          <span className="size-3 rounded-[3px] bg-gold-400" />
        </div>
        <div className="flex min-w-0 flex-col gap-4">
          <div>
            <p className="text-xs font-semibold text-ink-muted">Jemput</p>
            <p className="truncate text-[15px] font-bold">
              {offer.pickup.address}
            </p>
          </div>
          <div>
            <p className="text-xs font-semibold text-ink-muted">
              Antar ·{" "}
              {(offer.distanceMeters / 1000).toLocaleString("id-ID", {
                maximumFractionDigits: 1,
              })}{" "}
              km
            </p>
            <p className="truncate text-[15px] font-bold">
              {offer.dropoff.address}
            </p>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 text-[13px] font-semibold">
        <span className="rounded-lg bg-cream px-3 py-2">
          {CATEGORY[offer.itemCategory]}
        </span>
        <span className="rounded-lg bg-cream px-3 py-2">
          {offer.paymentMethod === "CASH"
            ? "Tunai — tagih penerima/pengirim"
            : "Sudah dibayar online"}
        </span>
      </div>

      <div className="grid grid-cols-[1fr_2fr] gap-2.5">
        <Button
          variant="secondary"
          onClick={onReject}
          disabled={busy || expired}
        >
          Tolak
        </Button>
        <Button onClick={onAccept} disabled={busy || expired}>
          {expired ? "Kedaluwarsa" : "Terima order"}
        </Button>
      </div>
    </article>
  );
}
