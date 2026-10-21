import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import AccountPage from "./AccountPage";
import { flash } from "../../../lib/flash";
import { fakeAuth } from "../../../test/auth";
import { API, customer, problem } from "../../../test/fixtures";
import { renderApp } from "../../../test/render";
import { server } from "../../../test/server";

const routes = [
  { path: "/account", element: <AccountPage /> },
  { path: "/login", element: <p>Halaman login</p> },
];

function setup() {
  const auth = fakeAuth({
    user: customer,
    updateUser: jest.fn(),
    logout: jest.fn().mockResolvedValue(undefined),
  });
  return { ...renderApp({ routes, url: "/account", auth }), auth };
}

afterEach(() => flash.consume());

describe("AccountPage — profil", () => {
  it("form terisi data user; tombol simpan nonaktif sampai ada perubahan", () => {
    setup();
    expect(screen.getByLabelText("Email")).toHaveValue(customer.email);
    expect(screen.getByLabelText("Nama lengkap")).toHaveValue(
      customer.fullName,
    );
    expect(
      screen.getByRole("button", { name: "Simpan profil" }),
    ).toBeDisabled();
  });

  it("simpan sukses → user di context diperbarui + banner sukses", async () => {
    server.use(
      http.patch(`${API}/api/v1/users/me`, async ({ request }) =>
        HttpResponse.json({
          ...customer,
          ...((await request.json()) as object),
        }),
      ),
    );
    const { user, auth } = setup();

    const name = screen.getByLabelText("Nama lengkap");
    await user.clear(name);
    await user.type(name, "Budi Baru");
    await user.click(screen.getByRole("button", { name: "Simpan profil" }));

    expect(await screen.findByText("Profil tersimpan")).toBeInTheDocument();
    expect(auth.updateUser).toHaveBeenCalledWith(
      expect.objectContaining({ fullName: "Budi Baru" }),
    );
  });

  it("PHONE_TAKEN dari backend → pesan di bawah field nomor HP", async () => {
    server.use(
      http.patch(`${API}/api/v1/users/me`, () =>
        problem(409, "PHONE_TAKEN", "Nomor HP sudah dipakai akun lain"),
      ),
    );
    const { user } = setup();

    const phone = screen.getByLabelText("Nomor HP");
    await user.clear(phone);
    await user.type(phone, "081298765432");
    await user.click(screen.getByRole("button", { name: "Simpan profil" }));

    expect(
      await screen.findByText("Nomor HP sudah dipakai akun lain"),
    ).toBeInTheDocument();
  });

  it("error yang tidak terkait field → banner error", async () => {
    server.use(
      http.patch(`${API}/api/v1/users/me`, () =>
        problem(500, "INTERNAL_ERROR", "Terjadi kesalahan di server"),
      ),
    );
    const { user } = setup();

    const name = screen.getByLabelText("Nama lengkap");
    await user.clear(name);
    await user.type(name, "Budi Baru");
    await user.click(screen.getByRole("button", { name: "Simpan profil" }));

    expect(
      await screen.findByText("Terjadi kesalahan di server"),
    ).toBeInTheDocument();
  });
});

describe("AccountPage — ganti password", () => {
  async function fillPasswords(
    user: ReturnType<typeof renderApp>["user"],
    current: string,
    next: string,
    confirm = next,
  ) {
    await user.type(screen.getByLabelText("Password saat ini"), current);
    await user.type(screen.getByLabelText("Password baru"), next);
    await user.type(screen.getByLabelText("Ulangi password baru"), confirm);
    await user.click(screen.getByRole("button", { name: "Ganti password" }));
  }

  it("konfirmasi tidak sama → ditolak sebelum dikirim", async () => {
    const { user } = setup();
    await fillPasswords(user, "lama12345", "baru12345", "beda12345");
    expect(
      await screen.findByText("Konfirmasi password tidak sama"),
    ).toBeInTheDocument();
  });

  it("password baru sama dengan yang lama → ditolak", async () => {
    const { user } = setup();
    await fillPasswords(user, "sama12345", "sama12345");
    expect(
      await screen.findByText("Password baru harus berbeda"),
    ).toBeInTheDocument();
  });

  it("sukses → pesan flash disiapkan, logout, lalu ke /login", async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/api/v1/users/me/password`, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user, auth, router } = setup();

    await fillPasswords(user, "lama12345", "baru12345");

    expect(await screen.findByText("Halaman login")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
    expect(auth.logout).toHaveBeenCalled();
    expect(body).toEqual({
      currentPassword: "lama12345",
      newPassword: "baru12345",
    }); // confirmPassword tidak dikirim
    expect(flash.consume()).toBe(
      "Password berhasil diganti. Silakan masuk dengan password baru.",
    );
  });

  it("INVALID_CURRENT_PASSWORD → pesan di bawah field password saat ini", async () => {
    server.use(
      http.post(`${API}/api/v1/users/me/password`, () =>
        problem(400, "INVALID_CURRENT_PASSWORD", "Password saat ini salah"),
      ),
    );
    const { user, auth } = setup();

    await fillPasswords(user, "salah1234", "baru12345");

    expect(
      await screen.findByText("Password saat ini salah"),
    ).toBeInTheDocument();
    expect(auth.logout).not.toHaveBeenCalled();
  });
});
