import FormField from './FormField.jsx'
import useFormField, { join_class_names } from './useFormField.js'

/**
 * label·설명·오류 메시지가 연결된 한 줄 입력창.
 * - 필수 입력은 isRequired 또는 required 중 하나만 true여도 적용된다.
 * - className은 기본 클래스에 더해지고, aria-describedby는 내부 설명·오류 ID와 합쳐진다.
 * - value·onChange·type·placeholder 등 나머지 props는 <input>에 그대로 전달된다.
 */
function InputField({
  id,
  label,
  description,
  error,
  isRequired = false,
  required = false,
  className,
  'aria-describedby': externalDescribedBy,
  ...inputProps
}) {
  const isFieldRequired = Boolean(isRequired || required)
  const fieldIds = useFormField({ id, description, error, externalDescribedBy })

  return (
    <FormField fieldIds={fieldIds} label={label} description={description} error={error} isRequired={isFieldRequired}>
      <input
        {...inputProps}
        id={fieldIds.fieldId}
        className={join_class_names('field__control', error && 'field__control--invalid', className)}
        required={isFieldRequired}
        aria-invalid={error ? true : inputProps['aria-invalid']}
        aria-describedby={fieldIds.describedBy}
      />
    </FormField>
  )
}

export default InputField
