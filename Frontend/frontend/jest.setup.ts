import "@testing-library/jest-dom";
import { randomUUID } from "node:crypto";
import { server } from "./src/test/server";

// jsdom belum lengkap mengimplementasikan <dialog>
const dialogProto = HTMLDialogElement.prototype as Partial<HTMLDialogElement>;
if (typeof dialogProto.showModal !== "function") {
  Object.assign(HTMLDialogElement.prototype, {
    showModal(this: HTMLDialogElement) {
      this.open = true;
    },
    close(this: HTMLDialogElement) {
      if (!this.open) return;
      this.open = false;
      this.dispatchEvent(new Event("close"));
    },
  });
}

// client.ts membuat X-Request-Id dengan crypto.randomUUID()
if (typeof globalThis.crypto.randomUUID !== "function") {
  Object.defineProperty(globalThis.crypto, "randomUUID", { value: randomUUID });
}

// MSW 3: onUnhandledRequest → onUnhandledFrame (mencakup request HTTP & koneksi WebSocket).
// 'error' = request yang tidak di-mock langsung gagal → test tidak pernah diam-diam memanggil jaringan sungguhan
beforeAll(() => server.listen({ onUnhandledFrame: "error" }));
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
});
afterAll(() => server.close());
