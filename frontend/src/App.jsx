import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import AdminAuthProvider from './auth/AdminAuthProvider.jsx'
import RequireAdminAuth from './auth/RequireAdminAuth.jsx'
import AdminLayout from './layouts/AdminLayout.jsx'
import CustomerLayout from './layouts/CustomerLayout.jsx'
import CustomerPage from './pages/CustomerPage.jsx'
import AdminLoginPage from './pages/admin/AdminLoginPage.jsx'
import AdminProductsPage from './pages/admin/AdminProductsPage.jsx'
import AdminOrdersPage from './pages/admin/AdminOrdersPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

// 공통 UI 확인용 화면. 개발 서버(npm run dev)에서만 /ui-guide 로 열리며 배포 빌드에는 포함되지 않는다.
const UiGuidePage = import.meta.env.DEV ? lazy(() => import('./pages/dev/UiGuidePage.jsx')) : null

// /admin/* 화면은 토큰이 있을 때만 보여 준다. 화면 보호는 편의 기능이고, 실제 권한 검사는 서버가 한다.
function App() {
  return (
    <AdminAuthProvider>
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

        <Route path="admin">
          {/* 공개 화면: 보호 라우트 밖에 두어 무한 이동을 막는다 */}
          <Route path="login" element={<AdminLoginPage />} />

          {/* 보호 화면: 토큰이 없으면 /admin/login 으로 replace 이동 */}
          <Route element={<RequireAdminAuth />}>
            <Route element={<AdminLayout />}>
              <Route index element={<Navigate to="products" replace />} />
              <Route path="products" element={<AdminProductsPage />} />
              <Route path="orders" element={<AdminOrdersPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Route>
        </Route>
      </Routes>
    </AdminAuthProvider>
  )
}

export default App
