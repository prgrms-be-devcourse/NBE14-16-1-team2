import FormField from './FormField.jsx'
import useFormField, { join_class_names } from './useFormField.js'

/**
 * label·설명·오류 메시지가 연결된 선택 상자.
 * options: [{ value, label }]
 * placeholder를 주면 값이 비어 있는 첫 항목으로 표시된다.
 * required·className·aria-describedby 처리 규칙은 InputField와 같다. 나머지 props는 <select>에 전달된다.
 */
function SelectField({
  id,
  label,
  description,
  error,
  isRequired = false,
  required = false,
  className,
  'aria-describedby': externalDescribedBy,
  options,
  placeholder,
  ...selectProps
}) {
  const isFieldRequired = Boolean(isRequired || required)
  const fieldIds = useFormField({ id, description, error, externalDescribedBy })

  return (
    <FormField fieldIds={fieldIds} label={label} description={description} error={error} isRequired={isFieldRequired}>
      <select
        {...selectProps}
        id={fieldIds.fieldId}
        className={join_class_names(
          'field__control',
          'field__control--select',
          error && 'field__control--invalid',
          className,
        )}
        required={isFieldRequired}
        aria-invalid={error ? true : selectProps['aria-invalid']}
        aria-describedby={fieldIds.describedBy}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </FormField>
  )
}

export default SelectField
