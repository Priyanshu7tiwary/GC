import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from './api.js';

const AuthContext = createContext(null);

/**
 * Source of truth for "who is signed in" is the server session (GET /api/auth/session),
 * never anything stored in the browser.
 */
export function AuthProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', user: null, googleClientId: null });

  const refresh = useCallback(async () => {
    try {
      const s = await api('/api/auth/session');
      setState({ status: s.authenticated ? 'authenticated' : 'anonymous', user: s.user || null, googleClientId: s.providers?.googleClientId || null });
    } catch {
      setState((prev) => ({ ...prev, status: 'anonymous', user: null }));
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const signedIn = useCallback((user) => setState((prev) => ({ ...prev, status: 'authenticated', user })), []);

  // Password sign-in and sign-up are two steps: these return a code challenge; verifyOtp starts the session.
  const login = useCallback((email, password) => api('/api/auth/login', { method: 'POST', body: { email, password } }), []);
  const register = useCallback((name, email, password) => api('/api/auth/register', { method: 'POST', body: { name, email, password } }), []);
  const verifyOtp = useCallback(async (challengeId, code) => signedIn(await api('/api/auth/otp/verify', { method: 'POST', body: { challengeId, code } })), [signedIn]);
  const resendOtp = useCallback((challengeId) => api('/api/auth/otp/resend', { method: 'POST', body: { challengeId } }), []);
  const loginWithGoogle = useCallback(async (credential) => signedIn(await api('/api/auth/google', { method: 'POST', body: { credential } })), [signedIn]);

  const logout = useCallback(async () => {
    try { await api('/api/auth/logout', { method: 'POST' }); } finally {
      setState((prev) => ({ ...prev, status: 'anonymous', user: null }));
      refresh(); // pick up a fresh CSRF token for the next sign-in
    }
  }, [refresh]);

  const value = useMemo(() => ({ ...state, login, register, verifyOtp, resendOtp, loginWithGoogle, logout, refresh }),
    [state, login, register, verifyOtp, resendOtp, loginWithGoogle, logout, refresh]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
