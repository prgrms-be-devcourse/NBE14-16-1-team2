import { Link } from 'react-router'
import { EmptyState, PageHeader } from '../components/common/index.js'

function NotFoundPage() {
  return (
    <section>
      <PageHeader title="404 - 페이지를 찾을 수 없습니다" />
      <EmptyState
        title="요청한 주소가 존재하지 않습니다."
        description="주소를 다시 확인하거나 아래 링크로 이동해 주세요."
        action={
          <Link to="/customer" className="btn btn--secondary">
            상품·주문 페이지로 이동
          </Link>
        }
      />
    </section>
  )
}

export default NotFoundPage
