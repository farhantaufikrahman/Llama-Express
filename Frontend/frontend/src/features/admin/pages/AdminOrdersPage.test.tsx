import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import AdminOrdersPage from "./AdminOrdersPage";
import { saveBlob } from "../../../lib/download";
import { API, pageOf, problem } from "../../../test/fixtures";
import { renderApp } from "../../../test/render";
import { server } from "../../../test/server";
import type { AdminOrderSummary } from "../types";

jest.mock("../../../lib/download", () => ({ saveBlob: jest.fn() })); // jsdom tidak bisa mengunduh file sungguhan

const row: AdminOrderSummary = {
  code: "LX-20261019-0001",
  status: "DELIVERED",
  serviceType: "INSTANT",
  customerName: "Budi Santoso",
  courierName: "Andi",
  priceTotal: 25000,
  paymentMethod: "CASH",
  paymentStatus: "PAID",
  createdAt: "2026-10-19T03:00:00Z",
};

/** Rekam setiap query string yang dikirim ke backend */
function mockOrders(totalElements = 1) {
  const requests: URLSearchParams[] = [];
  server.use(
    http.get(`${API}/api/v1/admin/orders`, ({ request }) => {
      const params = new URL(request.url).searchParams;
      requests.push(params);
      return HttpResponse.json(
        pageOf([row], { page: Number(params.get("page")), totalElements }),
      );
    }),
  );
  return requests;
}

const render = (url: string) =>
  renderApp({
    url,
    routes: [{ path: "/admin/orders", element: <AdminOrdersPage /> }],
  });
const search = (router: ReturnType<typeof render>["router"]) =>
  new URLSearchParams(router.state.location.search);

it("filter dari URL dipakai untuk request DAN ditampilkan di form", async () => {
  const requests = mockOrders();
  render("/admin/orders?status=DELIVERED&from=2026-10-01");

  expect(
    await screen.findByRole("link", { name: row.code }),
  ).toBeInTheDocument();
  expect(requests[0].get("status")).toBe("DELIVERED");
  expect(requests[0].get("from")).toBe("2026-10-01");
  expect(requests[0].get("size")).toBe("20");
  expect(screen.getByRole("combobox", { name: /^Status/ })).toHaveValue(
    "DELIVERED",
  );
});

it("mengganti filter memperbarui URL dan kembali ke halaman pertama", async () => {
  const requests = mockOrders(60);
  const { user, router } = render("/admin/orders?status=DELIVERED&page=2");
  await screen.findByRole("link", { name: row.code });

  await user.selectOptions(
    screen.getByRole("combobox", { name: /^Status/ }),
    "CANCELLED",
  );

  await waitFor(() => expect(search(router).get("status")).toBe("CANCELLED"));
  expect(search(router).has("page")).toBe(false);
  await waitFor(() => expect(requests.at(-1)?.get("page")).toBe("0"));
});

it("pagination: teks ringkasan + tombol Berikutnya", async () => {
  const requests = mockOrders(41);
  const { user, router } = render("/admin/orders");

  expect(
    await screen.findByText("Menampilkan 1–20 dari 41"),
  ).toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Berikutnya" }));

  await waitFor(() => expect(search(router).get("page")).toBe("1"));
  await waitFor(() => expect(requests.at(-1)?.get("page")).toBe("1"));
});

it("pencarian dikirim saat submit, bukan setiap ketikan", async () => {
  const requests = mockOrders();
  const { user, router } = render("/admin/orders");
  await screen.findByRole("link", { name: row.code });

  await user.type(
    screen.getByRole("textbox", { name: /^Cari/ }),
    "budi@mail.com",
  );
  expect(requests).toHaveLength(1); // mengetik saja tidak memicu request

  await user.click(screen.getByRole("button", { name: "Terapkan" }));
  await waitFor(() => expect(search(router).get("q")).toBe("budi@mail.com"));
});

it("ekspor CSV membawa filter aktif dan memakai nama file dari server", async () => {
  mockOrders();
  let exportParams: URLSearchParams | undefined;
  server.use(
    http.get(`${API}/api/v1/admin/orders/export`, ({ request }) => {
      exportParams = new URL(request.url).searchParams;
      return new HttpResponse("code\nLX-1\n", {
        headers: {
          "Content-Type": "text/csv",
          "Content-Disposition": 'attachment; filename="orders-2026-10-19.csv"',
        },
      });
    }),
  );
  const { user } = render("/admin/orders?status=DELIVERED");
  await screen.findByRole("link", { name: row.code });

  await user.click(screen.getByRole("button", { name: "Ekspor CSV" }));

  await waitFor(() =>
    expect(saveBlob).toHaveBeenCalledWith(
      expect.anything(),
      "orders-2026-10-19.csv",
    ),
  );
  expect(exportParams?.get("status")).toBe("DELIVERED");
});

it("ekspor gagal → pesan error backend ditampilkan", async () => {
  mockOrders();
  server.use(
    http.get(`${API}/api/v1/admin/orders/export`, () =>
      problem(403, "FORBIDDEN", "Akses ditolak"),
    ),
  );
  const { user } = render("/admin/orders");
  await screen.findByRole("link", { name: row.code });

  await user.click(screen.getByRole("button", { name: "Ekspor CSV" }));

  expect(await screen.findByText("Akses ditolak")).toBeInTheDocument();
  expect(saveBlob).not.toHaveBeenCalled();
});
