import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { refreshSession } from '../../lib/api/client';
import { tokenStore } from '../../lib/api/tokenStore';
import type { User } from '../../lib/api/types';
import { authApi } from './api';
import { AuthContext, type AuthStatus } from './AuthContext';

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

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password);
    tokenStore.set(tokens);
    const me = await authApi.me();
    setUser(me);
    setStatus('authenticated');
    return me;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefresh();
    tokenStore.clear();
    becomeAnonymous();
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined); // gagal di server pun, sesi lokal tetap berakhir
    }
  }, [becomeAnonymous]);

  const value = useMemo(() => ({ status, user, login, logout }), [status, user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}