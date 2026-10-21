import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import {
  CourierRuntimeContext,
  useCourierRuntime,
  type CourierRuntimeValue,
} from "./courierRuntimeContext";

describe("useCourierRuntime", () => {
  it("di luar <CourierRuntime> → error yang jelas (bukan undefined diam-diam)", () => {
    const spy = jest.spyOn(console, "error").mockImplementation(() => {}); // React mencetak error boundary ke console
    expect(() => renderHook(() => useCourierRuntime())).toThrow(
      "useCourierRuntime harus di dalam <CourierRuntime>",
    );
    spy.mockRestore();
  });

  it("di dalam provider → mengembalikan nilai context", () => {
    const value: CourierRuntimeValue = {
      profile: undefined,
      location: { status: "off", position: null, lastSentAt: null },
    };
    const wrapper = ({ children }: { children: ReactNode }) => (
      <CourierRuntimeContext.Provider value={value}>
        {children}
      </CourierRuntimeContext.Provider>
    );

    const { result } = renderHook(() => useCourierRuntime(), { wrapper });

    expect(result.current).toBe(value);
  });
});
