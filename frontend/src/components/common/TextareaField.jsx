import FormField from './FormField.jsx'
import useFormField, { join_class_names } from './useFormField.js'

/**
 * label·설명·오류 메시지가 연결된 여러 줄 입력창.
 * required·className·aria-describedby 처리 규칙은 InputField와 같다. 나머지 props는 <textarea>에 전달된다.
 */
function TextareaField({
  id,
  label,
  description,
  error,
  isRequired = false,
  required = false,
  className,
  'aria-describedby': externalDescribedBy,
  rows = 4,
  ...textareaProps
}) {
  const isFieldRequired = Boolean(isRequired || required)
  const fieldIds = useFormField({ id, description, error, externalDescribedBy })

  return (
    <FormField fieldIds={fieldIds} label={label} description={description} error={error} isRequired={isFieldRequired}>
      <textarea
        {...textareaProps}
        id={fieldIds.fieldId}
        rows={rows}
        className={join_class_names(
          'field__control',
          'field__control--textarea',
          error && 'field__control--invalid',
          className,
        )}
        required={isFieldRequired}
        aria-invalid={error ? true : textareaProps['aria-invalid']}
        aria-describedby={fieldIds.describedBy}
      />
    </FormField>
  )
}

export default TextareaField
