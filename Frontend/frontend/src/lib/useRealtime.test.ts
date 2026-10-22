import { renderHook } from "@testing-library/react";
import { useOnReconnect } from "./useRealtime";

jest.mock("./realtime", () => {
  const listeners = new Set<() => void>();
  return {
    onReconnect: jest.fn((listener: () => void) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    }),
    subscribe: jest.fn(() => () => {}),
    realtimeStatusStore: { subscribe: () => () => {}, get: () => "idle" },
    mockReconnect: () => listeners.forEach((listener) => listener()),
  };
});

const realtime = jest.requireMock<{
  onReconnect: jest.Mock;
  mockReconnect: () => void;
}>("./realtime");

describe("useOnReconnect", () => {
  it("tidak mendengarkan bila callback null", () => {
    renderHook(() => useOnReconnect(null));
    expect(realtime.onReconnect).not.toHaveBeenCalled();
  });

  it("memanggil callback TERBARU saat reconnect tanpa mendaftar ulang, dan berhenti saat unmount", () => {
    const first = jest.fn();
    const second = jest.fn();
    const { rerender, unmount } = renderHook(({ cb }) => useOnReconnect(cb), {
      initialProps: { cb: first },
    });

    rerender({ cb: second });
    realtime.mockReconnect();
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledTimes(1);
    expect(realtime.onReconnect).toHaveBeenCalledTimes(1); // re-render tidak memasang ulang listener

    unmount();
    realtime.mockReconnect();
    expect(second).toHaveBeenCalledTimes(1);
  });
});
