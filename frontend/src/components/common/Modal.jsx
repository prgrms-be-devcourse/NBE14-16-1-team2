import { useEffect, useId, useRef } from 'react'
import { createPortal } from 'react-dom'

const focusableSelector = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

/**
 * 공통 모달
 * - role="dialog" + aria-modal, 제목(h2)과 aria-labelledby 연결
 * - 열리면: 배경 스크롤 잠금, data-autofocus 요소 또는 첫 포커스 요소로 이동, Tab 순환
 * - Escape·닫기 버튼·바깥 영역 클릭 시 onClose 호출
 * - 닫히면: 스크롤 복구, 모달을 열었던 요소로 포커스 복귀
 */
function Modal({ isOpen, title, onClose, children, footer }) {
  const titleId = useId()
  const dialogRef = useRef(null)
  const onCloseRef = useRef(onClose)

  // 이벤트 리스너가 항상 최신 onClose를 호출하도록 보관
  useEffect(() => {
    onCloseRef.current = onClose
  })

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const dialogElement = dialogRef.current
    const previousFocusElement = document.activeElement
    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    const initialFocusElement =
      dialogElement.querySelector('[data-autofocus]') ?? dialogElement.querySelector(focusableSelector) ?? dialogElement
    initialFocusElement.focus()

    function handle_key_down(event) {
      if (event.key === 'Escape') {
        event.preventDefault()
        onCloseRef.current()
        return
      }

      if (event.key !== 'Tab') {
        return
      }

      const focusableElements = [...dialogElement.querySelectorAll(focusableSelector)]
      if (focusableElements.length === 0) {
        event.preventDefault()
        dialogElement.focus()
        return
      }

      const firstElement = focusableElements[0]
      const lastElement = focusableElements[focusableElements.length - 1]
      const isFocusOutside = !dialogElement.contains(document.activeElement)

      if (event.shiftKey && (document.activeElement === firstElement || document.activeElement === dialogElement || isFocusOutside)) {
        event.preventDefault()
        lastElement.focus()
      } else if (!event.shiftKey && (document.activeElement === lastElement || isFocusOutside)) {
        event.preventDefault()
        firstElement.focus()
      }
    }

    document.addEventListener('keydown', handle_key_down)

    return () => {
      document.removeEventListener('keydown', handle_key_down)
      document.body.style.overflow = previousBodyOverflow
      if (previousFocusElement instanceof HTMLElement) {
        previousFocusElement.focus()
      }
    }
  }, [isOpen])

  if (!isOpen) {
    return null
  }

  function handle_backdrop_mouse_down(event) {
    // 모달 안쪽이 아니라 바깥 어두운 영역을 눌렀을 때만 닫는다
    if (event.target === event.currentTarget) {
      onClose()
    }
  }

  return createPortal(
    <div className="modal-backdrop" onMouseDown={handle_backdrop_mouse_down}>
      <div ref={dialogRef} className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}>
        <div className="modal__header">
          <h2 id={titleId} className="modal__title">
            {title}
          </h2>
          <button type="button" className="icon-button" aria-label="닫기" onClick={onClose}>
            <span aria-hidden="true">×</span>
          </button>
        </div>
        <div className="modal__body">{children}</div>
        {footer && <div className="modal__footer">{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}

export default Modal
