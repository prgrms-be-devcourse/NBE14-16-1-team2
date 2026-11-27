import Button from './Button.jsx'
import Modal from './Modal.jsx'

/**
 * 확인·취소 두 버튼만 있는 확인 창 (예: 상품 삭제, 주문 취소).
 * - 처음 포커스는 안전한 "취소" 버튼에 둔다.
 * - isLoading 동안에는 닫히지 않고 확인 버튼이 비활성화된다.
 */
function ConfirmDialog({
  isOpen,
  title,
  message,
  confirmLabel = '확인',
  cancelLabel = '취소',
  variant = 'primary',
  isLoading = false,
  onConfirm,
  onCancel,
}) {
  function handle_close() {
    if (!isLoading) {
      onCancel()
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      title={title}
      onClose={handle_close}
      footer={
        <>
          <Button variant="secondary" onClick={handle_close} disabled={isLoading} data-autofocus>
            {cancelLabel}
          </Button>
          <Button variant={variant} onClick={onConfirm} isLoading={isLoading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p className="text-break">{message}</p>
    </Modal>
  )
}

export default ConfirmDialog
