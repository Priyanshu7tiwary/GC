import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AuthProvider, useAuth } from './lib/auth.jsx';
import { StoreProvider } from './lib/store.jsx';
import './styles.css';

// Progress is kept per account, so two people sharing a browser never see each other's data.
function UserScopedStore({ children }) {
  const { user } = useAuth();
  const id = user?.id || 'anonymous';
  return <StoreProvider key={id} userId={id}>{children}</StoreProvider>;
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider>
      <UserScopedStore>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </UserScopedStore>
    </AuthProvider>
  </React.StrictMode>,
);
