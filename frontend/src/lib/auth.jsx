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

  // Signs in directly, or returns a code challenge when the account's email still needs verifying.
  const login = useCallback(async (email, password) => {
    const r = await api('/api/auth/login', { method: 'POST', body: { email, password } });
    if (r?.challengeId) return r;
    signedIn(r);
    return null;
  }, [signedIn]);
  // Sign-up is two steps: register returns a code challenge and verifyOtp starts the session.
  const register = useCallback((name, email, password) => api('/api/auth/register', { method: 'POST', body: { name, email, password } }), []);
  const verifyOtp = useCallback(async (challengeId, code) => signedIn(await api('/api/auth/otp/verify', { method: 'POST', body: { challengeId, code } })), [signedIn]);
  const resendOtp = useCallback((challengeId) => api('/api/auth/otp/resend', { method: 'POST', body: { challengeId } }), []);
  const loginWithGoogle = useCallback(async (credential) => signedIn(await api('/api/auth/google', { method: 'POST', body: { credential } })), [signedIn]);
  // Forgot password: request a code by email, then set a new password with it (which also signs you in).
  const forgotPassword = useCallback((email) => api('/api/auth/password/forgot', { method: 'POST', body: { email } }), []);
  const resetPassword = useCallback(async (challengeId, code, password) =>
    signedIn(await api('/api/auth/password/reset', { method: 'POST', body: { challengeId, code, password } })), [signedIn]);

  const logout = useCallback(async () => {
    try { await api('/api/auth/logout', { method: 'POST' }); } finally {
      setState((prev) => ({ ...prev, status: 'anonymous', user: null }));
      refresh(); // pick up a fresh CSRF token for the next sign-in
    }
  }, [refresh]);

  const value = useMemo(() => ({ ...state, login, register, verifyOtp, resendOtp, loginWithGoogle, forgotPassword, resetPassword, logout, refresh }),
    [state, login, register, verifyOtp, resendOtp, loginWithGoogle, forgotPassword, resetPassword, logout, refresh]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
