import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useOnReconnect, useTopic } from "../../lib/useRealtime";
import { orderKeys } from "../orders/api";
import { trackingKeys } from "./api";
import type { TrackingSnapshot } from "./types";
import { useLiveOrder } from "./useLiveOrder";

jest.mock("../../lib/useRealtime", () => ({
  useTopic: jest.fn(),
  useOnReconnect: jest.fn(),
}));

function setup(code: string | null) {
  const queryClient = new QueryClient();
  const invalidate = jest.spyOn(queryClient, "invalidateQueries");
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  renderHook(() => useLiveOrder(code), { wrapper });
  const topicHandler = (suffix: string) =>
    jest
      .mocked(useTopic)
      .mock.calls.find(([destination]) => destination?.endsWith(suffix))?.[1];
  return { queryClient, invalidate, topicHandler };
}

describe("useLiveOrder", () => {
  it("setelah koneksi pulih → mengambil ulang detail order, tracking, dan daftar order", () => {
    const { invalidate } = setup("LX-1");
    const onReconnectCallback = jest
      .mocked(useOnReconnect)
      .mock.calls.at(-1)?.[0];
    expect(onReconnectCallback).toBeInstanceOf(Function);

    onReconnectCallback?.();
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: orderKeys.detail("LX-1"),
    });
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: trackingKeys.order("LX-1"),
    });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: orderKeys.list() });
  });

  it("event status memicu refresh yang sama", () => {
    const { invalidate, topicHandler } = setup("LX-1");
    topicHandler("/status")?.({});
    expect(invalidate).toHaveBeenCalledTimes(3);
  });

  it("event lokasi ditempel ke cache tracking tanpa request", () => {
    const { queryClient, invalidate, topicHandler } = setup("LX-1");
    queryClient.setQueryData(trackingKeys.order("LX-1"), {
      route: [],
      lastLocation: null,
    } as unknown as TrackingSnapshot);

    topicHandler("/location")?.({
      lat: -6.2,
      lng: 106.8,
      recordedAt: "2026-10-22T09:00:00Z",
    });

    const snapshot = queryClient.getQueryData<TrackingSnapshot>(
      trackingKeys.order("LX-1"),
    );
    expect(snapshot?.lastLocation).toEqual({
      lat: -6.2,
      lng: 106.8,
      at: "2026-10-22T09:00:00Z",
    });
    expect(snapshot?.route).toHaveLength(1);
    expect(invalidate).not.toHaveBeenCalled();
  });

  it("tanpa code → tidak berlangganan dan tidak mendengarkan reconnect", () => {
    setup(null);
    expect(jest.mocked(useOnReconnect)).toHaveBeenLastCalledWith(null);
    expect(
      jest
        .mocked(useTopic)
        .mock.calls.every(([destination]) => destination === null),
    ).toBe(true);
  });
});
