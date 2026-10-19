import { act, renderHook } from "@testing-library/react";
import { useDebouncedValue } from "./hooks";

beforeEach(() => jest.useFakeTimers());
afterEach(() => jest.useRealTimers());

it("nilai baru baru dipakai setelah jeda tanpa ketikan", () => {
  const { result, rerender } = renderHook(
    ({ v }) => useDebouncedValue(v, 500),
    { initialProps: { v: "a" } },
  );

  rerender({ v: "ab" });
  rerender({ v: "abc" });
  act(() => jest.advanceTimersByTime(499));
  expect(result.current).toBe("a"); // belum 500 ms → quote API belum dipanggil

  act(() => jest.advanceTimersByTime(1));
  expect(result.current).toBe("abc"); // hanya nilai TERAKHIR yang lolos
});
