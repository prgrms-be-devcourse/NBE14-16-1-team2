import { EmptyState, PageHeader } from '../../components/common/index.js'

function AdminProductsPage() {
  return (
    <section>
      <PageHeader title="상품 관리" description="원두 상품을 등록·수정·삭제합니다." />
      <EmptyState title="상품 관리 준비 중" description="상품 등록·수정·삭제 기능은 개발 예정입니다." />
    </section>
  )
}

export default AdminProductsPage
