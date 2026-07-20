import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    /*
      폰트는 절대 data URI로 인라인하지 않는다.

      Vite는 4KB(assetsInlineLimit 기본값) 미만 에셋을 자동으로 base64 data URI로 바꾼다. 폰트
      서브셋 중 작은 몇 개가 여기 걸리면서 배포 사이트에서 CSP 위반으로 차단됐다 —
      Caddyfile의 `font-src 'self'`는 `data:`를 허용하지 않는다(실측: 빌드 CSS에 data:font 4개,
      운영 콘솔 오류 4건과 일치).

      dev에서는 안 보인다: `npm run dev`는 에셋을 인라인하지 않고 CSP 헤더도 Caddy(운영)에만 있다.
      그래서 로컬은 멀쩡한데 배포만 깨졌다.

      CSP에 `data:`를 더하는 쪽이 아니라 인라인을 막는 쪽을 고른 이유: index.css가 폰트를 번들하며
      "self-host라 font-src 'self' 통과"라고 명시했다. 그 의도가 옳고, Vite의 기본값이 그걸 깬 것이다.
      보안 정책을 빌드 도구 편의에 맞춰 넓힐 이유가 없다.

      false = 인라인 금지, undefined = 나머지 에셋은 Vite 기본 동작 유지(작은 아이콘 등은 그대로 인라인).
    */
    assetsInlineLimit: (filePath) =>
      /\.(woff2?|ttf|otf|eot)$/i.test(filePath) ? false : undefined,
  },
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
