import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { refreshSession } from '../../lib/api/client';
import { tokenStore } from '../../lib/api/tokenStore';
import { authApi } from './api';
import { AuthContext, type AuthStatus } from './AuthContext';
import type { TokenResponse, User } from '../../lib/api/types';
import type { AuthContextValue } from './AuthContext';


export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<User | null>(null);

  const becomeAnonymous = useCallback(() => {
    setUser(null);
    setStatus('anonymous');
    queryClient.clear(); // data milik user sebelumnya tidak boleh terlihat user berikutnya
  }, [queryClient]);

  // Saat halaman dibuka/di-reload: access token hilang (hanya di memori) → pulihkan lewat refresh token
  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!tokenStore.getRefresh() || !(await refreshSession())) {
        if (!cancelled) setStatus('anonymous');
        return;
      }
      try {
        const me = await authApi.me();
        if (!cancelled) {
          setUser(me);
          setStatus('authenticated');
        }
      } catch {
        if (!cancelled) setStatus('anonymous');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => tokenStore.onSessionExpired(becomeAnonymous), [becomeAnonymous]);

  /** Satu pintu untuk "punya token baru → ambil profil → tandai login": dipakai login DAN register */
const establishSession = useCallback(async (tokens: TokenResponse) => {
  tokenStore.set(tokens);
  const me = await authApi.me();
  setUser(me);
  setStatus('authenticated');
  return me;
}, []);

const login = useCallback(
  async (email: string, password: string) => establishSession(await authApi.login(email, password)),
  [establishSession],
);
//   const login = useCallback(async (email: string, password: string) => {
//     const tokens = await authApi.login(email, password);
//     tokenStore.set(tokens);
//     const me = await authApi.me();
//     setUser(me);
//     setStatus('authenticated');
//     return me;
//   }, []);

const register = useCallback<AuthContextValue['register']>(
  async (request) =>
    establishSession(
      request.kind === 'customer'
        ? await authApi.registerCustomer(request.payload)
        : await authApi.registerCourier(request.payload),
    ),
  [establishSession],
);

const updateUser = useCallback((next: User) => setUser(next), []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefresh();
    tokenStore.clear();
    becomeAnonymous();
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined); // gagal di server pun, sesi lokal tetap berakhir
    }
  }, [becomeAnonymous]);

  const value = useMemo(
  () => ({ status, user, login, register, updateUser, logout }),
  [status, user, login, register, updateUser, logout],
);
//   const value = useMemo(() => ({ status, user, login, logout }), [status, user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
