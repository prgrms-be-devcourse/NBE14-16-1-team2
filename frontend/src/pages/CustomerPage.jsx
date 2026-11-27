import { EmptyState, PageHeader } from '../components/common/index.js'

function CustomerPage() {
  return (
    <section>
      <PageHeader title="원두 상품" description="원하는 원두를 골라 이메일과 배송지만 입력하면 주문할 수 있습니다." />
      <EmptyState title="상품 목록 준비 중" description="원두 상품 조회와 주문 기능은 개발 예정입니다." />
    </section>
  )
}

export default CustomerPage
