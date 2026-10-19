import { NavLink } from "react-router";
import { Logo } from "../components/Logo";
import { useAuth } from "../features/auth/AuthContext";
import type { Role } from "../lib/api/types";

const NAV: Partial<Record<Role, { to: string; label: string }[]>> = {
  CUSTOMER: [
    { to: "/orders", label: "Order" },
    { to: "/addresses", label: "Alamat" },
    { to: "/account", label: "Akun" },
  ],
  COURIER: [
    { to: "/courier", label: "Beranda" },
    { to: "/courier/job", label: "Tugas" },
    { to: "/courier/earnings", label: "Penghasilan" },
    { to: "/account", label: "Akun" },
  ],
};

export function AppHeader() {
  const { user, logout } = useAuth();
  const links = user ? (NAV[user.role] ?? []) : [];
  return (
    <header className="sticky top-0 z-10 border-b border-line bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between gap-3 px-4">
        <Logo size={32} withText={false} />
        <nav
          className="flex flex-1 gap-1 overflow-x-auto"
          aria-label="Menu utama"
        >
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end
              className={({
                isActive,
              }) => `whitespace-nowrap rounded-lg px-3 py-2 text-sm font-bold
                       ${isActive ? "bg-cream text-navy-900" : "text-ink-muted hover:text-navy-900"}`}
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <button
          onClick={logout}
          className="rounded-lg px-3 py-2 text-sm font-bold text-navy-900 hover:bg-cream"
        >
          Keluar
        </button>
      </div>
    </header>
  );
}
