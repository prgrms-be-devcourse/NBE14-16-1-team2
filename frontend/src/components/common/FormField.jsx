/**
 * InputField·TextareaField·SelectField가 함께 쓰는 label·설명·오류 메시지 틀.
 * 페이지에서 직접 쓰기보다는 세 필드 컴포넌트를 사용한다.
 */
function FormField({ fieldIds, label, description, error, isRequired, children }) {
  return (
    <div className="field">
      <label className="field__label" htmlFor={fieldIds.fieldId}>
        {label}
        {isRequired && <span className="field__required"> (필수)</span>}
      </label>
      {description && (
        <p id={fieldIds.descriptionId} className="field__description">
          {description}
        </p>
      )}
      {children}
      {error && (
        <p id={fieldIds.errorId} className="field__error">
          <span className="field__error-icon" aria-hidden="true">
            !
          </span>
          <span>
            <span className="visually-hidden">오류: </span>
            {error}
          </span>
        </p>
      )}
    </div>
  )
}

export default FormField
