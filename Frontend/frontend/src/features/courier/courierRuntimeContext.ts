import { createContext, useContext } from "react";
import type { CourierProfile } from "./types";
import type { LocationStatus } from "./useLocationReporter";

export interface CourierRuntimeValue {
  profile: CourierProfile | undefined;
  location: {
    status: LocationStatus;
    position: { lat: number; lng: number } | null;
    lastSentAt: number | null;
  };
}

export const CourierRuntimeContext = createContext<CourierRuntimeValue | null>(
  null,
);

export function useCourierRuntime(): CourierRuntimeValue {
  const ctx = useContext(CourierRuntimeContext);
  if (!ctx)
    throw new Error("useCourierRuntime harus di dalam <CourierRuntime>");
  return ctx;
}
