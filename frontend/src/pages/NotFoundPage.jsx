import { Link } from 'react-router'

function NotFoundPage() {
  return (
    <section>
      <h1>404 - 페이지를 찾을 수 없습니다</h1>
      <p>요청한 주소가 존재하지 않습니다.</p>
      <Link to="/customer">상품·주문 페이지로 이동</Link>
    </section>
  )
}

export default NotFoundPage
