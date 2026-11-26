import { Navigate, Route, Routes } from 'react-router'
import MainLayout from './layouts/MainLayout.jsx'
import CustomerPage from './pages/CustomerPage.jsx'
import AdminProductsPage from './pages/admin/AdminProductsPage.jsx'
import AdminOrdersPage from './pages/admin/AdminOrdersPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

// 주의: /admin/* 경로는 화면 구분일 뿐이며 인증·권한 검사를 하지 않는다.
function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<Navigate to="/customer" replace />} />
        <Route path="customer" element={<CustomerPage />} />
        <Route path="admin/products" element={<AdminProductsPage />} />
        <Route path="admin/orders" element={<AdminOrdersPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
