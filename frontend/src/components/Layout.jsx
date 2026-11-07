import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useCallback, useEffect, useState } from 'react';
import SuggestionsPanel from './SuggestionsPanel.jsx';
import { buildSuggestions } from '../lib/suggestions.js';
import { useStore, today, daysBetween, revisionBuckets } from '../lib/store.jsx';
import { useAuth } from '../lib/auth.jsx';
import { IconHome, IconTest, IconRevise, IconCoverage, IconChart, IconLogout, IconMoon, IconSun, IconCalendar, IconSparkle } from './Icons.jsx';

const NAV = [
  { to: '/', label: 'Dashboard', icon: IconHome, end: true },
  { to: '/mocks', label: 'Mock Tests', icon: IconTest },
  { to: '/revision', label: 'Revision', icon: IconRevise },
  { to: '/coverage', label: 'Coverage', icon: IconCoverage },
  { to: '/analytics', label: 'Analytics', icon: IconChart },
];

export function useTheme() {
  const [theme, setTheme] = useState(() => {
    try { return localStorage.getItem('catalyst-theme') || ''; } catch { return ''; }
  });
  useEffect(() => {
    if (theme) document.documentElement.dataset.theme = theme;
    else delete document.documentElement.dataset.theme;
    try { localStorage.setItem('catalyst-theme', theme); } catch { /* ignore */ }
  }, [theme]);
  const isDark = theme ? theme === 'dark' : window.matchMedia?.('(prefers-color-scheme: dark)').matches;
  return [isDark, () => setTheme(isDark ? 'light' : 'dark')];
}

export default function Layout() {
  const store = useStore();
  const { examDate, revisions } = store;
  const { user, logout } = useAuth();
  const [suggOpen, setSuggOpen] = useState(false);
  const closeSugg = useCallback(() => setSuggOpen(false), []);
  const highCount = buildSuggestions(store).filter((s) => s.priority === 'high').length;
  const nav = useNavigate();
  const [isDark, toggle] = useTheme();
  const days = daysBetween(today(), examDate);
  const { overdue, due } = revisionBuckets(revisions);
  const dueCount = overdue.length + due.length;

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark">C</div>CATalyst</div>
        {NAV.map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end} className="nav-link">
            <n.icon />{n.label}
            {n.to === '/revision' && dueCount > 0 && <span className="chip bad" style={{ marginLeft: 'auto', padding: '1px 7px' }}>{dueCount}</span>}
          </NavLink>
        ))}
        <button className="sugg-btn" onClick={() => setSuggOpen(true)}>
          <IconSparkle />
          <span style={{ flex: 1, textAlign: 'left' }}>
            <b>Get suggestions</b>
            <span className="small" style={{ display: 'block', opacity: 0.8 }}>Based on your analytics</span>
          </span>
          {highCount > 0 && <span className="sugg-badge">{highCount}</span>}
        </button>
        <div className="sidebar-foot">
          <div className="card" style={{ padding: 14, marginBottom: 10, background: 'var(--accent-soft)', border: 0 }}>
            <div className="small" style={{ color: 'var(--accent)', fontWeight: 600 }}>CAT {store.targetYear}</div>
            <div style={{ fontSize: 22, fontWeight: 700 }} className="num">{Math.max(0, days)} days</div>
            <div className="small muted">{new Date(examDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</div>
          </div>
          <button className="nav-link btn ghost" style={{ justifyContent: 'flex-start' }} onClick={toggle}>
            {isDark ? <IconSun /> : <IconMoon />}{isDark ? 'Light mode' : 'Dark mode'}
          </button>
          <button className="nav-link btn ghost" style={{ justifyContent: 'flex-start' }} onClick={async () => { await logout(); nav('/login', { replace: true }); }}>
            <IconLogout />Log out
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="chip accent"><IconCalendar width="14" height="14" />{Math.max(0, days)} days to CAT</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <button className="btn sm mobile-only" onClick={() => setSuggOpen(true)} aria-label="Suggestions"><IconSparkle />Tips</button>
            <button className="btn ghost sm mobile-only" onClick={toggle} aria-label="Toggle theme">{isDark ? <IconSun /> : <IconMoon />}</button>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontWeight: 600 }}>{user?.name}</div>
              <div className="small faint">{user?.email}</div>
            </div>
            <div className="brand-mark" style={{ borderRadius: '50%', width: 36, height: 36 }}>{user?.name?.[0]?.toUpperCase()}</div>
          </div>
        </header>
        <Outlet />
      </div>

      <SuggestionsPanel open={suggOpen} onClose={closeSugg} />

      <nav className="mobile-nav">
        {NAV.map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end} className="nav-link"><n.icon />{n.label.split(' ')[0]}</NavLink>
        ))}
      </nav>
    </div>
  );
}
