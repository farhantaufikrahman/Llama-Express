import type { env as realEnv } from "../config/env";

export const env: typeof realEnv = {
  apiBaseUrl: "http://api.test",
  wsUrl: "ws://api.test/ws",
  midtransClientKey: "SB-Mid-client-test",
  midtransSnapUrl: "https://app.sandbox.midtrans.com/snap/snap.js",
};
