import { Suspense } from 'react';
import { NavLink, Outlet } from 'react-router';
import { Logo } from '../components/Logo';
import { FullPageSpinner } from '../components/FullPageSpinner';
import { useAuth } from '../features/auth/AuthContext';

const nav = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/orders', label: 'Order' },
  { to: '/admin/couriers', label: 'Kurir' },
  { to: '/admin/users', label: 'Customer' },
  { to: '/admin/tariffs', label: 'Tarif' },
];

export function AdminLayout() {
  const { user, logout } = useAuth();
  return (
    <div className="flex min-h-dvh flex-col md:flex-row">
      <aside className="flex flex-col gap-1.5 bg-navy-900 px-4 py-6 text-cream md:w-60">
        <div className="px-2 pb-5"><Logo size={36} light /></div>
        {nav.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `rounded-xl px-3 py-3 text-sm font-semibold ${isActive ? 'bg-navy-800 font-bold text-gold-400' : 'text-[#C9D1E3] hover:bg-navy-800'}`}
          >
            {item.label}
          </NavLink>
        ))}
        <div className="mt-auto border-t border-navy-800 px-2 pt-4 text-sm">
          <p className="font-semibold">{user?.fullName}</p>
          <button onClick={logout} className="mt-2 font-bold text-gold-400 hover:underline">Keluar</button>
        </div>
      </aside>
      <main className="min-w-0 flex-1 p-4 md:p-7">
        <Suspense fallback={<FullPageSpinner />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  );
}