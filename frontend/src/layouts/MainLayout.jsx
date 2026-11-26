import { NavLink, Outlet } from 'react-router'

const navItems = [
  { path: '/customer', label: '상품·주문' },
  { path: '/admin/products', label: '관리자 상품 관리' },
  { path: '/admin/orders', label: '관리자 주문 관리' },
]

function MainLayout() {
  return (
    <div className="layout">
      <header className="layout-header">
        <span className="layout-title">카페 원두 주문</span>
        <nav className="layout-nav">
          {navItems.map((navItem) => (
            <NavLink key={navItem.path} to={navItem.path}>
              {navItem.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="layout-main">
        <Outlet />
      </main>
    </div>
  )
}

export default MainLayout
