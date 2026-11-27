import Button from './Button.jsx'

/** 데이터를 불러오는 중일 때 표시 */
export function LoadingState({ message = '불러오는 중입니다…' }) {
  return (
    <div className="state" role="status">
      <span className="spinner" aria-hidden="true" />
      <p className="state__description">{message}</p>
    </div>
  )
}

/** 목록이 비어 있을 때 표시. action에는 다음 행동 버튼·링크를 넣는다. */
export function EmptyState({ title, description, action }) {
  return (
    <div className="state">
      <p className="state__title">{title}</p>
      {description && <p className="state__description text-break">{description}</p>}
      {action && <div className="state__action">{action}</div>}
    </div>
  )
}

/** 오류가 났을 때 표시. onRetry를 넘기면 "다시 시도" 버튼이 나온다. */
export function ErrorState({ title = '문제가 발생했습니다', message, onRetry, isRetrying = false }) {
  return (
    <div className="state state--error" role="alert">
      <p className="state__title">
        <span className="state__error-icon" aria-hidden="true">
          !
        </span>
        <span>
          <span className="visually-hidden">오류: </span>
          {title}
        </span>
      </p>
      {message && <p className="state__description text-break">{message}</p>}
      {onRetry && (
        <div className="state__action">
          <Button variant="secondary" onClick={onRetry} isLoading={isRetrying}>
            다시 시도
          </Button>
        </div>
      )}
    </div>
  )
}
