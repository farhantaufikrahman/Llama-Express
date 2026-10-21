import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { courierApi, courierKeys } from "./api";
import { CourierRuntimeContext } from "./courierRuntimeContext";
import { useLocationReporter } from "./useLocationReporter";

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
