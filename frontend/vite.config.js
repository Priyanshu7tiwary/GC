import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the API is proxied so the app and API share one origin (cookies stay first-party, no CORS needed).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': { target: process.env.API_URL || 'http://localhost:8080', changeOrigin: false },
    },
  },
});
