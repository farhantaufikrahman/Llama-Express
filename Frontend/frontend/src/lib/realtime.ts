import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { env } from "../config/env";
import { ensureFreshAccessToken } from "./api/client";

export type RealtimeStatus =
  | "idle"
  | "connecting"
  | "connected"
  | "disconnected";

interface Entry {
  destination: string;
  handler: (body: unknown) => void;
  stomp?: StompSubscription;
}

let client: Client | null = null;
let status: RealtimeStatus = "idle";
let idleTimer: ReturnType<typeof setTimeout> | undefined;
let nextId = 1;
const entries = new Map<number, Entry>();
const statusListeners = new Set<() => void>();

function setStatus(next: RealtimeStatus) {
  status = next;
  statusListeners.forEach((l) => l());
}

function attach(entry: Entry) {
  entry.stomp = client!.subscribe(entry.destination, (msg: IMessage) => {
    try {
      entry.handler(JSON.parse(msg.body));
    } catch {
      // pesan rusak diabaikan — satu pesan buruk tidak boleh mematikan halaman
    }
  });
}

function ensureClient(): Client {
  if (client) return client;
  const c = new Client({
    brokerURL: env.wsUrl,
    reconnectDelay: 3_000, // putus → coba sambung lagi tiap 3 detik
    heartbeatIncoming: 10_000, // deteksi koneksi "zombie" (mis. Wi-Fi ganti) dalam ±10 detik
    heartbeatOutgoing: 10_000,
    // Dipanggil SEBELUM setiap percobaan connect (termasuk reconnect) → token selalu segar
    beforeConnect: async () => {
      setStatus("connecting");
      const token = await ensureFreshAccessToken();
      if (!token) {
        await disconnect(); // sesi habis → berhenti mencoba
        return;
      }
      c.connectHeaders = { Authorization: `Bearer ${token}` };
    },
    onConnect: () => {
      setStatus("connected");
      entries.forEach(attach); // langganan TIDAK otomatis pulih setelah reconnect → pasang ulang semuanya
    },
    onWebSocketClose: () => {
      entries.forEach((e) => (e.stomp = undefined));
      if (client) setStatus("disconnected");
    },
    onStompError: (frame) =>
      console.warn("[realtime] STOMP error:", frame.headers.message),
  });
  client = c;
  c.activate();
  return c;
}

/** Berlangganan satu topik. Mengembalikan fungsi untuk berhenti berlangganan. */
export function subscribe(
  destination: string,
  handler: (body: unknown) => void,
): () => void {
  clearTimeout(idleTimer);
  const id = nextId++;
  const entry: Entry = { destination, handler };
  entries.set(id, entry);
  const c = ensureClient();
  if (c.connected) attach(entry);

  return () => {
    entry.stomp?.unsubscribe();
    entries.delete(id);
    // Tutup koneksi kalau tidak dipakai lagi — dengan jeda, supaya pindah halaman tidak memutus-sambung
    if (entries.size === 0) {
      idleTimer = setTimeout(() => void disconnect(), 5_000);
    }
  };
}

export async function disconnect() {
  clearTimeout(idleTimer);
  const c = client;
  client = null;
  entries.forEach((e) => (e.stomp = undefined));
  setStatus("idle");
  await c?.deactivate();
}

export const realtimeStatusStore = {
  subscribe: (listener: () => void) => {
    statusListeners.add(listener);
    return () => statusListeners.delete(listener);
  },
  get: () => status,
};
