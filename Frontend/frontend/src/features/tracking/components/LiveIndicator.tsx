import { useRealtimeStatus } from "../../../lib/useRealtime";

export function LiveIndicator() {
  const status = useRealtimeStatus();
  const live = status === "connected";
  return (
    <span
      className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1.5 text-xs font-bold"
      role="status"
    >
      <span
        className={`size-2 rounded-full ${live ? "animate-pulse bg-success" : "bg-gold-400"}`}
      />
      {live ? "Live" : status === "idle" ? "Offline" : "Menyambung…"}
    </span>
  );
}
