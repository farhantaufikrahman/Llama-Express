import { Logo } from '../components/Logo';
import { useAuth } from '../features/auth/AuthContext';

export function AppHeader() {
  const { user, logout } = useAuth();
  return (
    <header className="sticky top-0 z-10 border-b border-line bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-4">
        <Logo size={32} />
        <div className="flex items-center gap-3">
          <span className="hidden text-sm font-semibold sm:inline">{user?.fullName}</span>
          <button onClick={logout} className="rounded-lg px-3 py-2 text-sm font-bold text-navy-900 hover:bg-cream">
            Keluar
          </button>
        </div>
      </div>
    </header>
  );
}