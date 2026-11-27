import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { Button } from '../components/common/index.js'
import { useAdminAuth } from '../auth/adminAuthContext.js'

// 관리자 메뉴. 현재 경로와 일치하는 항목은 NavLink가 active 클래스와 aria-current="page"를 붙인다.
const adminNavItems = [
  { path: '/admin/products', label: '상품 관리' },
  { path: '/admin/orders', label: '주문 관리' },
]

/**
 * 관리자용 공통 레이아웃: 관리자 헤더(로그아웃) · 메뉴(데스크톱 왼쪽 / 모바일 상단 탭) · 본문(Outlet)
 * 페이지 제목·작업 버튼은 각 페이지에서 PageHeader로 배치한다.
 * 로그인 확인은 이 레이아웃을 감싸는 RequireAdminAuth 라우트가 한다.
 */
function AdminLayout() {
  const { logout } = useAdminAuth()
  const navigate = useNavigate()

  // 브라우저의 토큰만 지운다. 서버 토큰은 만료 시각까지 유효하다. (로그아웃 API 없음)
  function handle_logout_click() {
    logout()
    navigate('/admin/login', { replace: true })
  }

  return (
    <div className="admin">
      <a className="skip-link" href="#main-content">
        본문 바로가기
      </a>

      <header className="admin-header">
        <div className="admin-header__inner">
          <Link to="/admin/products" className="admin-header__logo">
            카페 원두 주문 <span className="admin-header__badge">관리자</span>
          </Link>
          <div className="admin-header__actions">
            <Link to="/customer" className="admin-header__link">
              고객 화면 보기
            </Link>
            <Button variant="secondary" className="admin-header__logout" onClick={handle_logout_click}>
              로그아웃
            </Button>
          </div>
        </div>
      </header>

      <div className="admin-body">
        <nav className="admin-nav" aria-label="관리자 메뉴">
          <ul className="admin-nav__list">
            {adminNavItems.map((navItem) => (
              <li key={navItem.path}>
                <NavLink to={navItem.path} className="admin-nav__link">
                  {navItem.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <main id="main-content" className="admin-main" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
