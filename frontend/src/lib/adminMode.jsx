import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { useAuth } from './auth.jsx';

// Admins can switch between user mode (the normal learner experience) and admin mode, which shows each question's
// answer key and lets them change it. The server decides who is an admin; this only remembers the chosen mode.
const KEY = 'catalyst-admin-mode';
const AdminModeContext = createContext({ isAdmin: false, adminMode: false, setAdminMode: () => {} });

export function AdminModeProvider({ children }) {
  const { user } = useAuth();
  const isAdmin = !!user?.admin;
  const [on, setOn] = useState(() => {
    try { return localStorage.getItem(KEY) === '1'; } catch { return false; }
  });
  const setAdminMode = useCallback((value) => {
    setOn(value);
    try { localStorage.setItem(KEY, value ? '1' : '0'); } catch { /* ignore */ }
  }, []);
  const value = useMemo(() => ({ isAdmin, adminMode: isAdmin && on, setAdminMode }), [isAdmin, on, setAdminMode]);
  return <AdminModeContext.Provider value={value}>{children}</AdminModeContext.Provider>;
}

export const useAdminMode = () => useContext(AdminModeContext);

/** A two-state "User | Admin" switch, rendered only for admins. */
export function AdminModeSwitch({ compact = false }) {
  const { isAdmin, adminMode, setAdminMode } = useAdminMode();
  if (!isAdmin) return null;
  return (
    <div className="tabs" role="group" aria-label="View mode" style={{ flexShrink: 0 }}>
      <button className={`tab ${!adminMode ? 'active' : ''}`} aria-pressed={!adminMode} onClick={() => setAdminMode(false)}>{compact ? 'User' : 'User mode'}</button>
      <button className={`tab ${adminMode ? 'active' : ''}`} aria-pressed={adminMode} onClick={() => setAdminMode(true)}>{compact ? 'Admin' : 'Admin mode'}</button>
    </div>
  );
}
