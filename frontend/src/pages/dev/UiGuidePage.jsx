import { useState } from 'react'
import {
  Button,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  InputField,
  LoadingState,
  Modal,
  PageHeader,
  SelectField,
  TextareaField,
} from '../../components/common/index.js'
import './UiGuidePage.css'

/*
 * 공통 UI 확인용 화면 (개발 서버 전용: /ui-guide)
 * 아래 값은 모두 화면 확인용 예시이며 실제 API·DB와 관계없다.
 */
const sampleBeanOptions = [
  { value: 'sample-colombia', label: '(예시) 콜롬비아 수프리모' },
  { value: 'sample-ethiopia', label: '(예시) 에티오피아 예가체프' },
]

const longSampleText =
  '아주긴텍스트확인용문장입니다_띄어쓰기없이이어지는문자열도레이아웃을넘치지않아야합니다_https://example.com/very/long/path/for/layout/check'

const emptyFormValues = { email: '', zipCode: '', beanId: '', memo: '' }

function wait_for_sample(milliseconds) {
  return new Promise((resolve) => {
    setTimeout(resolve, milliseconds)
  })
}

function validate_sample_form(formValues) {
  const formErrors = {}
  if (!formValues.email.trim()) {
    formErrors.email = '이메일을 입력해 주세요.'
  } else if (!formValues.email.includes('@')) {
    formErrors.email = '이메일 형식이 올바르지 않습니다. 예) name@example.com'
  }
  if (!/^\d{5}$/.test(formValues.zipCode)) {
    formErrors.zipCode = '우편번호는 숫자 5자리로 입력해 주세요.'
  }
  if (!formValues.beanId) {
    formErrors.beanId = '원두를 선택해 주세요.'
  }
  return formErrors
}

function UiGuidePage() {
  const [isButtonLoading, setIsButtonLoading] = useState(false)
  const [formValues, setFormValues] = useState(emptyFormValues)
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitMessage, setSubmitMessage] = useState('')
  const [stateView, setStateView] = useState('loading')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isConfirmOpen, setIsConfirmOpen] = useState(false)
  const [isConfirmLoading, setIsConfirmLoading] = useState(false)

  async function handle_loading_button_click() {
    setIsButtonLoading(true)
    await wait_for_sample(1500)
    setIsButtonLoading(false)
  }

  function handle_field_change(event) {
    const { name, value } = event.target
    setFormValues((previousValues) => ({ ...previousValues, [name]: value }))
  }

  async function handle_sample_submit(event) {
    event.preventDefault()
    setSubmitMessage('')
    const nextErrors = validate_sample_form(formValues)
    setFormErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }
    setIsSubmitting(true)
    await wait_for_sample(1200)
    setIsSubmitting(false)
    setSubmitMessage('예시 제출이 끝났습니다. (실제로 저장되지 않습니다)')
  }

  async function handle_confirm_delete() {
    setIsConfirmLoading(true)
    await wait_for_sample(1200)
    setIsConfirmLoading(false)
    setIsConfirmOpen(false)
  }

  return (
    <section className="ui-guide">
      <PageHeader
        title="공통 UI 가이드"
        description="개발 서버에서만 열리는 확인용 화면입니다. 모든 데이터는 예시이며 실제 API를 호출하지 않습니다."
        actions={
          <>
            <Button variant="secondary" onClick={() => setIsModalOpen(true)}>
              모달 열기
            </Button>
            <Button variant="danger" onClick={() => setIsConfirmOpen(true)}>
              삭제 확인 열기
            </Button>
          </>
        }
      />

      <div className="ui-guide__section">
        <h2 className="ui-guide__heading">Button</h2>
        <div className="ui-guide__row">
          <Button>기본 동작</Button>
          <Button variant="secondary">보조 동작</Button>
          <Button variant="danger">위험 동작</Button>
          <Button disabled>비활성</Button>
          <Button isLoading={isButtonLoading} onClick={handle_loading_button_click}>
            {isButtonLoading ? '저장 중' : '눌러서 로딩 확인'}
          </Button>
          <button type="button" className="icon-button" aria-label="예시 아이콘 버튼">
            <span aria-hidden="true">×</span>
          </button>
        </div>
      </div>

      <div className="ui-guide__section">
        <h2 className="ui-guide__heading">입력 필드</h2>
        <form className="ui-guide__form" noValidate onSubmit={handle_sample_submit}>
          <InputField
            label="이메일"
            name="email"
            type="email"
            autoComplete="email"
            description="주문 확인에 사용합니다."
            placeholder="name@example.com"
            isRequired
            value={formValues.email}
            onChange={handle_field_change}
            error={formErrors.email}
          />
          <InputField
            label="우편번호"
            name="zipCode"
            inputMode="numeric"
            isRequired
            value={formValues.zipCode}
            onChange={handle_field_change}
            error={formErrors.zipCode}
          />
          <SelectField
            label="원두"
            name="beanId"
            placeholder="원두를 선택하세요"
            options={sampleBeanOptions}
            isRequired
            value={formValues.beanId}
            onChange={handle_field_change}
            error={formErrors.beanId}
          />
          <TextareaField
            label="요청 사항"
            name="memo"
            description="선택 입력입니다."
            value={formValues.memo}
            onChange={handle_field_change}
          />
          <InputField label="비활성 입력" value="수정할 수 없는 값" disabled readOnly />
          <div className="ui-guide__row">
            <Button type="submit" isLoading={isSubmitting}>
              예시 제출
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                setFormValues(emptyFormValues)
                setFormErrors({})
                setSubmitMessage('')
              }}
            >
              초기화
            </Button>
          </div>
          <p role="status" className="ui-guide__note">
            {submitMessage}
          </p>
        </form>
      </div>

      <div className="ui-guide__section">
        <h2 className="ui-guide__heading">상태 표시</h2>
        <div className="ui-guide__row" role="group" aria-label="상태 표시 선택">
          {[
            ['loading', '로딩'],
            ['empty', '빈 목록'],
            ['error', '오류'],
            ['long', '긴 텍스트'],
          ].map(([viewName, viewLabel]) => (
            <Button
              key={viewName}
              variant={stateView === viewName ? 'primary' : 'secondary'}
              aria-pressed={stateView === viewName}
              onClick={() => setStateView(viewName)}
            >
              {viewLabel}
            </Button>
          ))}
        </div>
        {stateView === 'loading' && <LoadingState message="상품 목록을 불러오는 중입니다…" />}
        {stateView === 'empty' && (
          <EmptyState
            title="등록된 상품이 없습니다"
            description="첫 원두 상품을 등록해 보세요."
            action={<Button variant="secondary">예시 동작 버튼</Button>}
          />
        )}
        {stateView === 'error' && (
          <ErrorState message="서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요." onRetry={() => setStateView('loading')} />
        )}
        {stateView === 'long' && (
          <>
            <PageHeader
              title="아주 긴 페이지 제목이 들어와도 줄바꿈되어 작업 버튼과 겹치지 않는지 확인하는 예시 제목입니다"
              description={longSampleText}
              actions={<Button>작업 버튼</Button>}
            />
            <EmptyState title={longSampleText} description={longSampleText} />
          </>
        )}
      </div>

      <Modal
        isOpen={isModalOpen}
        title="예시 모달"
        onClose={() => setIsModalOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>
              닫기
            </Button>
            <Button onClick={() => setIsModalOpen(false)}>확인</Button>
          </>
        }
      >
        <p>Tab으로 모달 안에서만 포커스가 이동하고, Escape로 닫히는지 확인합니다.</p>
        <InputField label="예시 입력" placeholder="아무 값이나 입력" />
      </Modal>

      <ConfirmDialog
        isOpen={isConfirmOpen}
        title="상품을 삭제할까요?"
        message="(예시) 삭제한 상품은 고객 화면에 더 이상 표시되지 않습니다."
        confirmLabel="삭제"
        variant="danger"
        isLoading={isConfirmLoading}
        onConfirm={handle_confirm_delete}
        onCancel={() => setIsConfirmOpen(false)}
      />
    </section>
  )
}

export default UiGuidePage
