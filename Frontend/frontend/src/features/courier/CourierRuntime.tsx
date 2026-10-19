import { createContext, useContext, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { courierApi, courierKeys } from "./api";
import type { CourierProfile } from "./types";
import {
  useLocationReporter,
  type LocationStatus,
} from "./useLocationReporter";

interface CourierRuntimeValue {
  profile: CourierProfile | undefined;
  location: {
    status: LocationStatus;
    position: { lat: number; lng: number } | null;
    lastSentAt: number | null;
  };
}

const CourierRuntimeContext = createContext<CourierRuntimeValue | null>(null);

/**
 * Dipasang di LAYOUT (bukan di satu halaman) → pengiriman GPS tetap jalan
 * saat kurir berpindah Beranda ↔ Tugas ↔ Penghasilan ↔ Akun.
 */
export function CourierRuntime({ children }: { children: ReactNode }) {
  const { data: profile } = useQuery({
    queryKey: courierKeys.me,
    queryFn: ({ signal }) => courierApi.me(signal),
  });
  const location = useLocationReporter(profile?.online === true);
  return (
    <CourierRuntimeContext.Provider value={{ profile, location }}>
      {children}
    </CourierRuntimeContext.Provider>
  );
}

export function useCourierRuntime(): CourierRuntimeValue {
  const ctx = useContext(CourierRuntimeContext);
  if (!ctx)
    throw new Error("useCourierRuntime harus di dalam <CourierRuntime>");
  return ctx;
}
