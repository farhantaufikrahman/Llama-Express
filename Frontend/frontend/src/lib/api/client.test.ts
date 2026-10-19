import { delay, http, HttpResponse } from "msw";
import { server } from "../../test/server";
import {
  API,
  REFRESH_PATH,
  fakeJwt,
  problem,
  tokens,
} from "../../test/fixtures";

const REFRESH_KEY = "llama.refreshToken";
const NEW_ACCESS = fakeJwt({ role: "CUSTOMER" });

/** Access token tersimpan di memori modul → modul segar = sesi browser segar */
async function loadClient(refreshToken?: string) {
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
  jest.resetModules();
  const [client, errors] = await Promise.all([
    import("./client"),
    import("./errors"),
  ]);
  return { ...client, ...errors };
}

/** Meniru backend langkah 3: refresh token sekali pakai + reuse detection */
function backendWithRotation() {
  const calls = { refresh: 0 };
  const used = new Set<string>();
  server.use(
    http.post(`${API}${REFRESH_PATH}`, async ({ request }) => {
      calls.refresh++;
      const { refreshToken } = (await request.json()) as {
        refreshToken: string;
      };
      if (refreshToken !== "r1" || used.has(refreshToken)) {
        return problem(401, "INVALID_REFRESH_TOKEN"); // dipakai dua kali = dianggap dicuri
      }
      used.add(refreshToken);
      await delay(30); // refresh lambat → request lain sempat "menumpuk"
      return HttpResponse.json(tokens(NEW_ACCESS, "r2"));
    }),
    http.get(`${API}/api/v1/orders`, ({ request }) =>
      request.headers.get("Authorization") === `Bearer ${NEW_ACCESS}`
        ? HttpResponse.json({ ok: true })
        : problem(401, "UNAUTHORIZED"),
    ),
  );
  return calls;
}

describe("api() — refresh token", () => {
  it("401 → refresh sekali → request diulang dengan token baru", async () => {
    const calls = backendWithRotation();
    const { api } = await loadClient("r1");

    await expect(api("/api/v1/orders")).resolves.toEqual({ ok: true });
    expect(calls.refresh).toBe(1);
    expect(localStorage.getItem(REFRESH_KEY)).toBe("r2"); // token dirotasi
  });

  it("SINGLE-FLIGHT: 3 request 401 bersamaan hanya memicu 1 refresh", async () => {
    const calls = backendWithRotation();
    const { api } = await loadClient("r1");

    const results = await Promise.all([
      api("/api/v1/orders"),
      api("/api/v1/orders"),
      api("/api/v1/orders"),
    ]);

    // Tanpa single-flight: r1 dikirim 3× → reuse detection backend mencabut sesi → user ter-logout
    expect(calls.refresh).toBe(1);
    expect(results).toEqual([{ ok: true }, { ok: true }, { ok: true }]);
  });

  it("refresh ditolak → sesi dibersihkan & error 401 diteruskan", async () => {
    backendWithRotation();
    const { api } = await loadClient("r-sudah-dicabut");

    await expect(api("/api/v1/orders")).rejects.toMatchObject({ status: 401 });
    expect(localStorage.getItem(REFRESH_KEY)).toBeNull();
  });

  it("tanpa refresh token → tidak mencoba refresh sama sekali", async () => {
    const calls = backendWithRotation();
    const { api } = await loadClient();

    await expect(api("/api/v1/orders")).rejects.toMatchObject({ status: 401 });
    expect(calls.refresh).toBe(0);
  });
});

describe("api() — request & error", () => {
  it("mengirim JSON + X-Request-Id, tanpa Authorization bila auth:false", async () => {
    let seen: Headers | undefined;
    server.use(
      http.post(`${API}/api/v1/auth/login`, ({ request }) => {
        seen = request.headers;
        return HttpResponse.json(tokens(NEW_ACCESS, "r1"));
      }),
    );
    const { api } = await loadClient();

    await api("/api/v1/auth/login", {
      method: "POST",
      body: { email: "a@b.c", password: "x" },
      auth: false,
    });

    expect(seen?.get("Content-Type")).toBe("application/json");
    expect(seen?.get("X-Request-Id")).toMatch(/^[0-9a-f-]{36}$/);
    expect(seen?.has("Authorization")).toBe(false);
  });

  it("ProblemDetail backend → ApiError dengan status, code, dan pesan", async () => {
    server.use(
      http.post(`${API}/api/v1/admin/tariffs`, () =>
        problem(409, "TARIFF_OVERLAP", "Tarif bertabrakan"),
      ),
    );
    const { api, ApiError } = await loadClient();

    const call = api("/api/v1/admin/tariffs", {
      method: "POST",
      body: {},
      auth: false,
    });

    await expect(call).rejects.toBeInstanceOf(ApiError);
    await expect(call).rejects.toMatchObject({
      status: 409,
      code: "TARIFF_OVERLAP",
      message: "Tarif bertabrakan",
    });
  });
});

describe("apiDownload()", () => {
  it("mengembalikan blob + nama file dari Content-Disposition", async () => {
    server.use(
      http.get(
        `${API}/api/v1/admin/orders/export`,
        () =>
          new HttpResponse("code,status\nLX-1,DELIVERED\n", {
            headers: {
              "Content-Type": "text/csv",
              "Content-Disposition":
                'attachment; filename="orders-2026-10-19.csv"',
            },
          }),
      ),
    );
    const { apiDownload } = await loadClient();

    const { blob, filename } = await apiDownload(
      "/api/v1/admin/orders/export",
      { auth: false },
    );

    expect(filename).toBe("orders-2026-10-19.csv");
    expect(await blob.text()).toContain("LX-1,DELIVERED");
  });
});
