import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './lib/auth.jsx';
import Layout, { useTheme } from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import MockTests from './pages/MockTests.jsx';
import MockSetup from './pages/MockSetup.jsx';
import TestPlayer from './pages/TestPlayer.jsx';
import Result from './pages/Result.jsx';
import Revision from './pages/Revision.jsx';
import Coverage from './pages/Coverage.jsx';
import Analytics from './pages/Analytics.jsx';

function RequireAuth({ children }) {
  const { status } = useAuth();
  const loc = useLocation();
  if (status === 'loading') return <div className="boot">Checking your session…</div>;
  return status === 'authenticated' ? children : <Navigate to="/login" state={{ from: loc.pathname + loc.search }} replace />;
}

export default function App() {
  useTheme(); // apply saved theme on every route, including login & test player
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/test/:mockId" element={<RequireAuth><TestPlayer /></RequireAuth>} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route index element={<Dashboard />} />
        <Route path="mocks" element={<MockTests />} />
        <Route path="mocks/start/:mockId" element={<MockSetup />} />
        <Route path="mocks/result/:attemptId" element={<Result />} />
        <Route path="revision" element={<Revision />} />
        <Route path="coverage" element={<Coverage />} />
        <Route path="analytics" element={<Analytics />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
