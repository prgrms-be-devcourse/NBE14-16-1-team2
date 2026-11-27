import { Link, NavLink, Outlet } from 'react-router'

// 고객 화면 상단 메뉴. 고객 기능 페이지가 추가되면 여기에 항목을 추가한다.
const customerNavItems = [{ path: '/customer', label: '원두 상품·주문' }]

/**
 * 고객용 공통 레이아웃: 공지 바 · 헤더(로고·메뉴) · 본문(Outlet) · 푸터
 * 페이지별 데이터 조회·주문 로직은 이 레이아웃에 넣지 않고 각 페이지에서 처리한다.
 */
function CustomerLayout() {
  return (
    <div className="site">
      <a className="skip-link" href="#main-content">
        본문 바로가기
      </a>

      <div className="notice-bar">
        <p className="container">주문 시각 오후 2시를 기준으로 배송 예정일이 정해집니다.</p>
      </div>

      <header className="site-header">
        <div className="container site-header__inner">
          <Link to="/customer" className="site-logo">
            카페 원두 주문
          </Link>
          <nav aria-label="주요 메뉴">
            <ul className="site-nav">
              {customerNavItems.map((navItem) => (
                <li key={navItem.path}>
                  <NavLink to={navItem.path} className="site-nav__link">
                    {navItem.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </header>

      <main id="main-content" className="site-main" tabIndex={-1}>
        <div className="container">
          <Outlet />
        </div>
      </main>

      <footer className="site-footer">
        <div className="container site-footer__inner">
          <div className="site-footer__intro">
            <p className="site-footer__brand">카페 원두 주문</p>
            <p className="site-footer__text">회원가입 없이 이메일로 원두를 주문할 수 있습니다.</p>
          </div>
          <div>
            <h2 className="site-footer__heading">주문 안내</h2>
            <ul className="site-footer__list">
              <li>오후 2시 기준으로 배송 예정일을 계산합니다.</li>
              <li>주문 후에는 고객이 직접 취소할 수 없습니다.</li>
            </ul>
          </div>
          <div>
            <h2 className="site-footer__heading">바로가기</h2>
            <ul className="site-footer__list">
              <li>
                <Link to="/customer">원두 상품·주문</Link>
              </li>
              <li>
                <Link to="/admin/products">관리자 화면</Link>
              </li>
            </ul>
          </div>
        </div>
        <div className="site-footer__bottom">
          <p className="container">© 2026 카페 원두 주문 · NBE14-16-1 team2</p>
        </div>
      </footer>
    </div>
  )
}

export default CustomerLayout
