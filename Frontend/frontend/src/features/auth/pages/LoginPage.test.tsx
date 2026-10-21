import { screen } from "@testing-library/react";
import LoginPage from "./LoginPage";
import { flash } from "../../../lib/flash";
import { fakeAuth, userWithRole } from "../../../test/auth";
import { apiError } from "../../../test/fixtures";
import { renderApp } from "../../../test/render";

const routes = [
  { path: "/login", element: <LoginPage /> },
  { path: "/orders", element: <p>Beranda customer</p> },
  { path: "/admin/orders", element: <p>Kelola order</p> },
];

function setup(
  login: jest.Mock,
  url: Parameters<typeof renderApp>[0]["url"] = "/login",
) {
  const auth = fakeAuth({ status: "anonymous", user: null, login });
  return renderApp({ routes, url, auth });
}

async function fillAndSubmit(
  user: ReturnType<typeof renderApp>["user"],
  email = "budi@mail.com",
  password = "rahasia123",
) {
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Masuk" }));
}

afterEach(() => flash.consume()); // pesan flash bersifat global → jangan bocor ke test lain

describe("LoginPage", () => {
  it("validasi di browser: field kosong → pesan error, backend tidak dipanggil", async () => {
    const login = jest.fn();
    const { user } = setup(login);

    await user.click(screen.getByRole("button", { name: "Masuk" }));

    expect(await screen.findByText("Email wajib diisi")).toBeInTheDocument();
    expect(screen.getByText("Password wajib diisi")).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it("login sukses → ke beranda sesuai role", async () => {
    const login = jest.fn().mockResolvedValue(userWithRole("CUSTOMER"));
    const { user } = setup(login);

    await fillAndSubmit(user);

    expect(login).toHaveBeenCalledWith("budi@mail.com", "rahasia123");
    expect(await screen.findByText("Beranda customer")).toBeInTheDocument();
  });

  it("login sukses → kembali ke halaman asal (state.from dari guard)", async () => {
    const login = jest.fn().mockResolvedValue(userWithRole("ADMIN"));
    const { user } = setup(login, {
      pathname: "/login",
      state: { from: "/admin/orders" },
    });

    await fillAndSubmit(user);

    expect(await screen.findByText("Kelola order")).toBeInTheDocument();
  });

  it('401 → pesan "Email atau password salah" (tidak membocorkan field mana yang salah)', async () => {
    const login = jest
      .fn()
      .mockRejectedValue(
        apiError(401, "INVALID_CREDENTIALS", "Kredensial tidak valid"),
      );
    const { user } = setup(login);

    await fillAndSubmit(user);

    expect(
      await screen.findByText("Email atau password salah"),
    ).toBeInTheDocument();
  });

  it("ApiError lain (mis. akun dibekukan) → pesan dari backend", async () => {
    const login = jest
      .fn()
      .mockRejectedValue(
        apiError(403, "ACCOUNT_SUSPENDED", "Akun kamu dinonaktifkan"),
      );
    const { user } = setup(login);

    await fillAndSubmit(user);

    expect(
      await screen.findByText("Akun kamu dinonaktifkan"),
    ).toBeInTheDocument();
  });

  it("error jaringan → pesan koneksi", async () => {
    const login = jest.fn().mockRejectedValue(new TypeError("Failed to fetch"));
    const { user } = setup(login);

    await fillAndSubmit(user);

    expect(
      await screen.findByText(/Tidak bisa terhubung ke server/),
    ).toBeInTheDocument();
  });

  it("menampilkan pesan flash dari halaman sebelumnya (mis. setelah ganti password)", async () => {
    flash.set("Password berhasil diganti. Silakan masuk dengan password baru.");
    setup(jest.fn());

    expect(
      await screen.findByText(
        "Password berhasil diganti. Silakan masuk dengan password baru.",
      ),
    ).toBeInTheDocument();
  });
});
