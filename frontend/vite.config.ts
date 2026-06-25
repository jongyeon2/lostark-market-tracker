import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      // shadcn/ui blocks import from `@/...`; keep this in sync with tsconfig.app.json paths.
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    proxy: {
      // FND-02 / D-10: browser sees same-origin `/api/*`; Vite forwards to the Spring backend.
      // Overridable via VITE_API_TARGET so a reviewer can repoint without editing tracked files.
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
