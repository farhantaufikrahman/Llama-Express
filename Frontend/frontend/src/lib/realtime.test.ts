import { ensureFreshAccessToken } from "./api/client";
import {
  disconnect,
  onReconnect,
  realtimeStatusStore,
  subscribe,
} from "./realtime";

jest.mock("./api/client", () => ({ ensureFreshAccessToken: jest.fn() }));

// Client STOMP palsu: menyimpan konfigurasi (beforeConnect, onConnect, dst.) supaya test bisa
// mensimulasikan connect → putus → reconnect tanpa server WebSocket sungguhan.
jest.mock("@stomp/stompjs", () => {
  const instances: unknown[] = [];
  class Client {
    config: unknown;
    connected = false;
    connectHeaders: Record<string, string> = {};
    activate = jest.fn();
    deactivate = jest.fn(() => Promise.resolve());
    subscribe = jest.fn(() => ({ unsubscribe: jest.fn() }));
    constructor(config: unknown) {
      this.config = config;
      instances.push(this);
    }
  }
  return { Client, mockInstances: instances };
});

interface FakeClient {
  config: {
    beforeConnect: () => Promise<void>;
    onConnect: () => void;
    onWebSocketClose: () => void;
  };
  connected: boolean;
  connectHeaders: Record<string, string>;
  activate: jest.Mock;
  deactivate: jest.Mock;
  subscribe: jest.Mock;
}

const { mockInstances } = jest.requireMock<{ mockInstances: FakeClient[] }>(
  "@stomp/stompjs",
);
const latestClient = () => mockInstances[mockInstances.length - 1];
const freshToken = ensureFreshAccessToken as unknown as jest.Mock<
  Promise<string | null>,
  []
>;

beforeEach(() => jest.useFakeTimers());

afterEach(async () => {
  await disconnect();
  mockInstances.length = 0;
  jest.useRealTimers();
});

describe("realtime", () => {
  it("reconnect: memasang ulang langganan, LALU memberi tahu listener (koneksi pertama tidak)", () => {
    const listener = jest.fn();
    const stopListening = onReconnect(listener);
    const unsubscribe = subscribe("/topic/orders/LX-1/status", jest.fn());
    const client = latestClient();
    expect(client.activate).toHaveBeenCalledTimes(1);

    client.config.onConnect(); // koneksi pertama
    expect(realtimeStatusStore.get()).toBe("connected");
    expect(client.subscribe).toHaveBeenCalledTimes(1);
    expect(listener).not.toHaveBeenCalled();

    client.config.onWebSocketClose(); // mis. pod backend diganti saat deploy
    expect(realtimeStatusStore.get()).toBe("disconnected");

    client.config.onConnect(); // tersambung kembali
    expect(client.subscribe).toHaveBeenCalledTimes(2);
    expect(listener).toHaveBeenCalledTimes(1);
    // Urutan penting: subscribe dulu, baru refetch → event di antara keduanya tidak hilang
    expect(client.subscribe.mock.invocationCallOrder[1]).toBeLessThan(
      listener.mock.invocationCallOrder[0],
    );

    stopListening();
    client.config.onWebSocketClose();
    client.config.onConnect();
    expect(listener).toHaveBeenCalledTimes(1); // sudah berhenti mendengarkan

    unsubscribe();
  });

  it("koneksi pertama client BARU setelah disconnect tidak dianggap reconnect", async () => {
    const listener = jest.fn();
    const stopListening = onReconnect(listener);

    const unsubscribeFirst = subscribe("/topic/a", jest.fn());
    const first = latestClient();
    first.config.onConnect();
    unsubscribeFirst();
    await disconnect();
    expect(first.deactivate).toHaveBeenCalled();
    expect(realtimeStatusStore.get()).toBe("idle");

    const unsubscribeSecond = subscribe("/topic/a", jest.fn());
    const second = latestClient();
    expect(second).not.toBe(first);
    second.config.onConnect();
    expect(listener).not.toHaveBeenCalled();

    unsubscribeSecond();
    stopListening();
  });

  it("meneruskan body JSON ke handler, mengabaikan pesan rusak, dan langsung attach bila sudah connected", () => {
    const handler = jest.fn();
    const unsubscribe = subscribe("/topic/x", handler);
    const client = latestClient();
    client.config.onConnect();

    const deliver = client.subscribe.mock.calls[0][1] as (msg: {
      body: string;
    }) => void;
    deliver({ body: '{"status":"PICKED_UP"}' });
    deliver({ body: "bukan json" });
    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler).toHaveBeenCalledWith({ status: "PICKED_UP" });

    client.connected = true;
    const unsubscribeLate = subscribe("/topic/y", jest.fn());
    expect(client.subscribe).toHaveBeenCalledTimes(2);

    unsubscribeLate();
    unsubscribe();
  });

  it("beforeConnect memasang token segar; tanpa token → berhenti mencoba", async () => {
    const unsubscribe = subscribe("/topic/x", jest.fn());
    const client = latestClient();

    freshToken.mockResolvedValueOnce("tok-1");
    await client.config.beforeConnect();
    expect(client.connectHeaders).toEqual({ Authorization: "Bearer tok-1" });

    freshToken.mockResolvedValueOnce(null);
    await client.config.beforeConnect();
    expect(client.deactivate).toHaveBeenCalled();
    expect(realtimeStatusStore.get()).toBe("idle");

    unsubscribe();
  });

  it("menutup koneksi 5 detik setelah langganan terakhir dilepas", () => {
    const unsubscribe = subscribe("/topic/x", jest.fn());
    const client = latestClient();
    unsubscribe();

    jest.advanceTimersByTime(4_999);
    expect(client.deactivate).not.toHaveBeenCalled();
    jest.advanceTimersByTime(1);
    expect(client.deactivate).toHaveBeenCalled();
  });

  it("memberi tahu listener saat status koneksi berubah", () => {
    const listener = jest.fn();
    const stop = realtimeStatusStore.subscribe(listener);
    const unsubscribe = subscribe("/topic/x", jest.fn());
    latestClient().config.onConnect();
    expect(listener).toHaveBeenCalled();
    stop();
    unsubscribe();
  });
});
