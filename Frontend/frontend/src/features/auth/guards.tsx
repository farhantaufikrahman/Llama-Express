import { Navigate, Outlet, useLocation } from 'react-router';
import type { Role } from '../../lib/api/types';
import { FullPageSpinner } from '../../components/FullPageSpinner';
import { useAuth } from './AuthContext';
import { homeFor } from './roles';

/**
 * Guard di frontend hanya untuk PENGALAMAN PENGGUNA (tidak menampilkan halaman yang salah).
 * KEAMANAN sebenarnya tetap di backend: SecurityConfig menolak dengan 401/403 apa pun yang dilakukan di browser.
 */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { status, user } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <FullPageSpinner />;
  if (status === 'anonymous' || !user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />;
  return <Outlet />;
}

/** Halaman login: kalau sudah login, langsung lempar ke beranda role-nya */
export function GuestOnly() {
  const { status, user } = useAuth();
  if (status === 'loading') return <FullPageSpinner />;
  if (status === 'authenticated' && user) return <Navigate to={homeFor(user.role)} replace />;
  return <Outlet />;
}

export function RoleRedirect() {
  const { status, user } = useAuth();
  if (status === 'loading') return <FullPageSpinner />;
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />;
}