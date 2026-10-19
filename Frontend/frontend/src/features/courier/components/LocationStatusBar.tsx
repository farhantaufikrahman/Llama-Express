import type { LocationStatus } from "../useLocationReporter";

const TEXT: Record<LocationStatus, { tone: string; text: string }> = {
  off: { tone: "bg-line", text: "Lokasi tidak dibagikan (offline)" },
  locating: { tone: "animate-pulse bg-gold-400", text: "Mencari sinyal GPS…" },
  sharing: { tone: "animate-pulse bg-success", text: "Lokasi dibagikan" },
  denied: {
    tone: "bg-danger",
    text: "Izin lokasi ditolak — aktifkan di pengaturan browser agar bisa menerima order",
  },
  unavailable: {
    tone: "bg-danger",
    text: "GPS tidak tersedia di perangkat/browser ini",
  },
};

export function LocationStatusBar({
  status,
  lastSentAt,
}: {
  status: LocationStatus;
  lastSentAt: number | null;
}) {
  const t = TEXT[status];
  return (
    <p className="flex items-center gap-2 text-sm" role="status">
      <span className={`size-2.5 shrink-0 rounded-full ${t.tone}`} />
      <span>{t.text}</span>
      {status === "sharing" && lastSentAt && (
        <span className="text-ink-muted">
          · terkirim {new Date(lastSentAt).toLocaleTimeString("id-ID")}
        </span>
      )}
    </p>
  );
}
