import { useId } from 'react'

// 클래스 이름들을 공백으로 이어 붙인다. undefined·false·빈 문자열은 제외한다.
export function join_class_names(...classNames) {
  return classNames.filter(Boolean).join(' ')
}

// 공백으로 구분된 여러 ID 묶음을 하나로 합친다. 중복은 제거하고, 결과가 없으면 undefined를 돌려준다.
export function merge_id_lists(...idLists) {
  const mergedIds = new Set()
  for (const idList of idLists) {
    if (!idList) {
      continue
    }
    for (const idValue of String(idList).split(/\s+/)) {
      if (idValue) {
        mergedIds.add(idValue)
      }
    }
  }
  return mergedIds.size > 0 ? [...mergedIds].join(' ') : undefined
}

/**
 * 입력 필드의 id와 설명·오류 메시지 연결(aria-describedby)을 만든다.
 * externalDescribedBy: 호출자가 직접 넘긴 aria-describedby (내부 설명·오류 ID와 함께 연결된다)
 */
function useFormField({ id, description, error, externalDescribedBy }) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  const descriptionId = `${fieldId}-description`
  const errorId = `${fieldId}-error`
  const describedBy = merge_id_lists(externalDescribedBy, description && descriptionId, error && errorId)

  return { fieldId, descriptionId, errorId, describedBy }
}

export default useFormField
