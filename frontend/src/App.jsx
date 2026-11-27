import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import AdminLayout from './layouts/AdminLayout.jsx'
import CustomerLayout from './layouts/CustomerLayout.jsx'
import CustomerPage from './pages/CustomerPage.jsx'
import AdminProductsPage from './pages/admin/AdminProductsPage.jsx'
import AdminOrdersPage from './pages/admin/AdminOrdersPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

// 공통 UI 확인용 화면. 개발 서버(npm run dev)에서만 /ui-guide 로 열리며 배포 빌드에는 포함되지 않는다.
const UiGuidePage = import.meta.env.DEV ? lazy(() => import('./pages/dev/UiGuidePage.jsx')) : null

// 주의: /admin/* 경로는 화면 구분일 뿐이며 인증·권한 검사를 하지 않는다.
function App() {
  return (
    <Routes>
      <Route index element={<Navigate to="/customer" replace />} />

      <Route element={<CustomerLayout />}>
        <Route path="customer" element={<CustomerPage />} />
        {UiGuidePage && (
          <Route
            path="ui-guide"
            element={
              <Suspense fallback={null}>
                <UiGuidePage />
              </Suspense>
            }
          />
        )}
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route path="admin" element={<AdminLayout />}>
        <Route index element={<Navigate to="products" replace />} />
        <Route path="products" element={<AdminProductsPage />} />
        <Route path="orders" element={<AdminOrdersPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
