import { useNow } from "../../../lib/useNow";
import type { CourierInfo, LocationPoint } from "../types";

export function CourierCard({
  courier,
  lastLocation,
}: {
  courier: CourierInfo;
  lastLocation: LocationPoint | null;
}) {
  const now = useNow(1_000);
  const seconds = lastLocation
    ? Math.max(
        0,
        Math.round((now - new Date(lastLocation.at).getTime()) / 1000),
      )
    : null;

  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl bg-white p-4">
      <div className="flex items-center gap-3">
        <div className="grid size-11 place-items-center rounded-full bg-navy-900 text-lg font-extrabold text-gold-400">
          {courier.name.charAt(0).toUpperCase()}
        </div>
        <div>
          <p className="font-bold">{courier.name}</p>
          <p className="text-xs text-ink-muted">
            {courier.vehicleType === "MOTOR" ? "Motor" : "Mobil"} ·{" "}
            {courier.plateNumber}
            {courier.ratingAvg != null &&
              ` · ★ ${Number(courier.ratingAvg).toFixed(1)}`}
          </p>
          {seconds != null && (
            <p className="text-xs text-ink-muted">
              Posisi diperbarui{" "}
              {seconds < 60
                ? `${seconds} dtk`
                : `${Math.round(seconds / 60)} mnt`}{" "}
              lalu
            </p>
          )}
        </div>
      </div>
      {courier.phone && (
        <a
          href={`tel:${courier.phone}`}
          className="rounded-xl bg-cream px-3.5 py-2.5 text-sm font-bold text-navy-900"
        >
          Telepon
        </a>
      )}
    </div>
  );
}
