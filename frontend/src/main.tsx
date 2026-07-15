import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'

import { queryClient } from '@/lib/queryClient'
import { AppLayout } from '@/components/layout/AppLayout'
import { RootErrorBoundary } from '@/components/RootErrorBoundary'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { TimelinePage } from '@/features/timeline/TimelinePage'
import { AdminRoute } from '@/features/admin/AdminRoute'
import './index.css'

/*
  D-03: React Router with real paths so refresh / deep-link / browser-back all work
  (in-memory tab switching rejected). /dashboard · /timeline · /impact under a persistent
  AppLayout shell; / redirects to /dashboard. Structured so Phase 9-10 can add filter state
  as URL searchParams (D-04) without restructuring the route tree.
*/
const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    errorElement: <RootErrorBoundary />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'timeline', element: <TimelinePage /> },
      // /gems·/impact 는 Phase 28에서 대시보드로 흡수됐다. 라우트는 redirect로 남긴다 —
      // 북마크·기존 링크가 404로 죽지 않게(경로를 지우는 것과 목적지를 옮기는 것은 다르다).
      { path: 'impact', element: <Navigate to="/dashboard" replace /> },
      { path: 'gems', element: <Navigate to="/dashboard" replace /> },
    ],
  },
  // Admin console (D-04): a SIBLING top-level route — a separate shell with NO public TopNav, reached
  // by direct URL only. The public /dashboard·/timeline·/impact branch above is untouched.
  { path: '/admin', element: <AdminRoute /> },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)