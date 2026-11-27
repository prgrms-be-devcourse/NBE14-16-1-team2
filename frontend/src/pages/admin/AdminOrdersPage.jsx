import { EmptyState, PageHeader } from '../../components/common/index.js'

function AdminOrdersPage() {
  return (
    <section>
      <PageHeader title="주문 관리" description="접수된 주문과 배송 상태를 확인합니다." />
      <EmptyState title="주문 관리 준비 중" description="주문 조회·처리 기능은 개발 예정입니다." />
    </section>
  )
}

export default AdminOrdersPage
