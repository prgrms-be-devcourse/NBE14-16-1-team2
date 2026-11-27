const variantClassNames = {
  primary: 'btn--primary',
  secondary: 'btn--secondary',
  danger: 'btn--danger',
}

/**
 * 공통 버튼
 * - type 기본값은 "button". 폼 제출 버튼만 type="submit"으로 지정한다.
 * - isLoading이면 비활성화되어 중복 클릭을 막는다.
 * - 아이콘만 있는 버튼은 aria-label을 꼭 넘긴다.
 */
function Button({
  variant = 'primary',
  type = 'button',
  isLoading = false,
  disabled = false,
  className = '',
  children,
  ...restProps
}) {
  const buttonClassName = ['btn', variantClassNames[variant] ?? variantClassNames.primary, className]
    .filter(Boolean)
    .join(' ')

  return (
    <button
      {...restProps}
      type={type}
      className={buttonClassName}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
    >
      {isLoading && <span className="spinner spinner--sm" aria-hidden="true" />}
      <span>{children}</span>
      {isLoading && <span className="visually-hidden">(처리 중)</span>}
    </button>
  )
}

export default Button
