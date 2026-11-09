import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './lib/auth.jsx';
import TopBar from './components/TopBar.jsx';
import Layout, { useTheme } from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import MockTests from './pages/MockTests.jsx';
import MockSetup from './pages/MockSetup.jsx';
import Revision from './pages/Revision.jsx';
import Coverage from './pages/Coverage.jsx';
import Analytics from './pages/Analytics.jsx';
import QuestionBank from './pages/QuestionBank.jsx';

// The test player and result pages render maths with KaTeX, so they load on demand.
const TestPlayer = lazy(() => import('./pages/TestPlayer.jsx'));
const Result = lazy(() => import('./pages/Result.jsx'));
const TopicPractice = lazy(() => import('./pages/TopicPractice.jsx'));
const Loading = () => <div className="boot"><div className="loading-card"><span className="ring" /><span>Loading…</span></div></div>;

function RequireAuth({ children }) {
  const { status } = useAuth();
  const loc = useLocation();
  if (status === 'loading') return <div className="boot"><div className="loading-card"><span className="ring" /><span>Checking your session…</span></div></div>;
  return status === 'authenticated' ? children : <Navigate to="/login" state={{ from: loc.pathname + loc.search }} replace />;
}

export default function App() {
  useTheme(); // apply saved theme on every route, including login & test player
  return (
    <>
    <TopBar />
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/test/:mockId" element={<RequireAuth><Suspense fallback={<Loading />}><TestPlayer /></Suspense></RequireAuth>} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route index element={<Dashboard />} />
        <Route path="mocks" element={<MockTests />} />
        <Route path="mocks/start/:mockId" element={<MockSetup />} />
        <Route path="mocks/result/:attemptId" element={<Suspense fallback={<Loading />}><Result /></Suspense>} />
        <Route path="revision" element={<Revision />} />
        <Route path="coverage" element={<Coverage />} />
        <Route path="practice/:topicId" element={<Suspense fallback={<Loading />}><TopicPractice /></Suspense>} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="admin/questions" element={<QuestionBank />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
    </>
  );
}
