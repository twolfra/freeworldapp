import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { auth as authApi } from '../api/client';
import { getStoredUser, setStoredUser, clearStoredUser } from './authStorage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getStoredUser());

  const login = useCallback(async (credentials) => {
    const loggedIn = await authApi.login(credentials);
    setStoredUser(loggedIn);
    setUser(loggedIn);
    return loggedIn;
  }, []);

  const logout = useCallback(async ({ skipServer = false } = {}) => {
    // Best-effort server-side session invalidation; always clear locally.
    // skipServer: when the session is already gone (e.g. account deleted),
    // calling the endpoint would 401 and hard-redirect to /login.
    if (!skipServer) await authApi.logout().catch(() => {});
    clearStoredUser();
    setUser(null);
  }, []);

  const updateUser = useCallback((partial) => {
    setUser((prev) => {
      if (!prev) return prev;
      const next = { ...prev, ...partial };
      setStoredUser(next);
      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ user, login, logout, updateUser }),
    [user, login, logout, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components -- provider + hook belong together; auth state lives in localStorage so a fast-refresh remount is harmless
export function useAuth() {
  return useContext(AuthContext);
}
