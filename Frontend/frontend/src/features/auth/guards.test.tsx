import { screen } from "@testing-library/react";
import { RequireRole } from "./guards";
import { fakeAuth, userWithRole } from "../../test/auth";
import { renderApp } from "../../test/render";

const routes = [
  {
    element: <RequireRole roles={["ADMIN"]} />,
    children: [{ path: "/admin", element: <p>Panel admin</p> }],
  },
  { path: "/login", element: <p>Halaman login</p> },
  { path: "/courier", element: <p>Beranda kurir</p> },
];

describe("RequireRole", () => {
  it("pengunjung anonim diarahkan ke /login", async () => {
    renderApp({
      routes,
      url: "/admin",
      auth: fakeAuth({ status: "anonymous", user: null }),
    });
    expect(await screen.findByText("Halaman login")).toBeInTheDocument();
  });

  it("role lain diarahkan ke berandanya sendiri, bukan ke 403", async () => {
    renderApp({
      routes,
      url: "/admin",
      auth: fakeAuth({ user: userWithRole("COURIER") }),
    });
    expect(await screen.findByText("Beranda kurir")).toBeInTheDocument();
  });

  it("role yang sesuai boleh masuk", async () => {
    renderApp({
      routes,
      url: "/admin",
      auth: fakeAuth({ user: userWithRole("ADMIN") }),
    });
    expect(await screen.findByText("Panel admin")).toBeInTheDocument();
  });

  it("selama sesi dipulihkan (refresh di awal) tidak redirect ke login", () => {
    renderApp({
      routes,
      url: "/admin",
      auth: fakeAuth({ status: "loading", user: null }),
    });
    expect(screen.queryByText("Halaman login")).not.toBeInTheDocument();
  });
});
