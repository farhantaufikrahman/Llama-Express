import { screen } from "@testing-library/react";
import RegisterPage from "./RegisterPage";
import { fakeAuth, userWithRole } from "../../../test/auth";
import { apiError } from "../../../test/fixtures";
import { renderApp } from "../../../test/render";

const routes = [
  { path: "/register", element: <RegisterPage mode="customer" /> },
  { path: "/register/courier", element: <RegisterPage mode="courier" /> },
  { path: "/orders", element: <p>Beranda customer</p> },
  { path: "/courier", element: <p>Beranda kurir</p> },
  { path: "/login", element: <p>Halaman login</p> },
];

function setup(url: string, register: jest.Mock) {
  const auth = fakeAuth({ status: "anonymous", user: null, register });
  return renderApp({ routes, url, auth });
}

type User = ReturnType<typeof renderApp>["user"];

async function fillAccount(user: User, { confirm = "rahasia123" } = {}) {
  await user.type(screen.getByLabelText("Nama lengkap"), "Budi Santoso");
  await user.type(screen.getByLabelText("Email"), "budi@mail.com");
  await user.type(screen.getByLabelText("Nomor HP"), "081234567890");
  await user.type(screen.getByLabelText("Password"), "rahasia123");
  await user.type(screen.getByLabelText("Ulangi password"), confirm);
}

const account = {
  fullName: "Budi Santoso",
  email: "budi@mail.com",
  phone: "081234567890",
  password: "rahasia123",
};

describe("RegisterPage — customer", () => {
  it('tab "Customer" aktif', () => {
    setup("/register", jest.fn());
    expect(screen.getByRole("link", { name: "Customer" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(screen.getByRole("link", { name: "Kurir" })).not.toHaveAttribute(
      "aria-current",
    );
  });

  it("konfirmasi password berbeda → ditolak sebelum dikirim", async () => {
    const register = jest.fn();
    const { user } = setup("/register", register);

    await fillAccount(user, { confirm: "beda12345" });
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai customer" }),
    );

    expect(
      await screen.findByText("Konfirmasi password tidak sama"),
    ).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it("sukses → payload TANPA confirmPassword, lalu ke beranda customer", async () => {
    const register = jest.fn().mockResolvedValue(userWithRole("CUSTOMER"));
    const { user } = setup("/register", register);

    await fillAccount(user);
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai customer" }),
    );

    expect(await screen.findByText("Beranda customer")).toBeInTheDocument();
    expect(register).toHaveBeenCalledWith({
      kind: "customer",
      payload: account,
    });
  });

  it("EMAIL_TAKEN dari backend → pesan di bawah field email, bukan banner", async () => {
    const register = jest
      .fn()
      .mockRejectedValue(apiError(409, "EMAIL_TAKEN", "Email sudah terdaftar"));
    const { user } = setup("/register", register);

    await fillAccount(user);
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai customer" }),
    );

    expect(
      await screen.findByText("Email sudah terdaftar"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
  });

  it("error yang tidak terkait field → tampil sebagai banner", async () => {
    const register = jest
      .fn()
      .mockRejectedValue(
        apiError(
          429,
          "RATE_LIMITED",
          "Terlalu banyak percobaan, coba lagi nanti",
        ),
      );
    const { user } = setup("/register", register);

    await fillAccount(user);
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai customer" }),
    );

    expect(
      await screen.findByText("Terlalu banyak percobaan, coba lagi nanti"),
    ).toBeInTheDocument();
  });
});

describe("RegisterPage — kurir", () => {
  it("sukses → SIM kosong TIDAK dikirim, kendaraan terpilih ikut, lalu ke beranda kurir", async () => {
    const register = jest.fn().mockResolvedValue(userWithRole("COURIER"));
    const { user } = setup("/register/courier", register);

    await fillAccount(user);
    await user.click(screen.getByLabelText("Mobil"));
    await user.type(screen.getByLabelText("Nomor plat"), "D 1234 ABC");
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai kurir" }),
    );

    expect(await screen.findByText("Beranda kurir")).toBeInTheDocument();
    expect(register).toHaveBeenCalledWith({
      kind: "courier",
      payload: {
        ...account,
        vehicleType: "CAR",
        plateNumber: "D 1234 ABC",
        licenseNo: undefined,
      },
    });
  });

  it("nomor SIM diisi → ikut dikirim", async () => {
    const register = jest.fn().mockResolvedValue(userWithRole("COURIER"));
    const { user } = setup("/register/courier", register);

    await fillAccount(user);
    await user.type(screen.getByLabelText("Nomor plat"), "B 1 XY");
    await user.type(screen.getByLabelText("Nomor SIM (opsional)"), "SIM-123");
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai kurir" }),
    );

    expect(await screen.findByText("Beranda kurir")).toBeInTheDocument();
    expect(register).toHaveBeenCalledWith({
      kind: "courier",
      payload: {
        ...account,
        vehicleType: "MOTOR",
        plateNumber: "B 1 XY",
        licenseNo: "SIM-123",
      },
    });
  });

  it("format plat salah → ditolak sebelum dikirim", async () => {
    const register = jest.fn();
    const { user } = setup("/register/courier", register);

    await fillAccount(user);
    await user.type(screen.getByLabelText("Nomor plat"), "1234");
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai kurir" }),
    );

    expect(
      await screen.findByText("Format plat tidak valid (contoh: D 1234 ABC)"),
    ).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it("PLATE_TAKEN dari backend → pesan di bawah field plat", async () => {
    const register = jest
      .fn()
      .mockRejectedValue(apiError(409, "PLATE_TAKEN", "Plat sudah terdaftar"));
    const { user } = setup("/register/courier", register);

    await fillAccount(user);
    await user.type(screen.getByLabelText("Nomor plat"), "D 1234 ABC");
    await user.click(
      screen.getByRole("button", { name: "Daftar sebagai kurir" }),
    );

    expect(await screen.findByText("Plat sudah terdaftar")).toBeInTheDocument();
  });
});
