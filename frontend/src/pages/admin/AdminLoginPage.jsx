import { useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { Button, InputField } from '../../components/common/index.js'
import { useAdminAuth } from '../../auth/adminAuthContext.js'

const loginErrorId = 'admin-login-error'

// 로그인 실패 원인을 화면 문구로 바꾼다. (비밀번호 값은 문구·로그에 넣지 않는다)
function get_login_error_message(error) {
  if (error?.kind === 'config') {
    return '로그인 서버 주소가 설정되지 않았습니다. 개발 환경 설정(VITE_API_BASE_URL)을 확인해 주세요.'
  }
  if (error?.kind === 'network') {
    return '서버에 연결할 수 없습니다. 네트워크 상태나 서버 실행 여부를 확인한 뒤 다시 시도해 주세요.'
  }
  if (error?.kind === 'invalid_response') {
    return '서버 응답을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }
  if (error?.status === 401) {
    return '비밀번호가 올바르지 않습니다. 다시 확인해 주세요.'
  }
  if (error?.status === 400) {
    return '비밀번호를 입력해 주세요. 공백만으로는 로그인할 수 없습니다.'
  }
  if (error?.status >= 500) {
    return `서버 오류로 로그인하지 못했습니다. 잠시 후 다시 시도해 주세요. (HTTP ${error.status})`
  }
  if (error?.status) {
    return `로그인 요청을 처리하지 못했습니다. (HTTP ${error.status})`
  }
  return '알 수 없는 오류로 로그인하지 못했습니다. 다시 시도해 주세요.'
}

/** 관리자 로그인 화면 (/admin/login). 관리자 헤더·메뉴 없이 단독으로 표시한다. */
function AdminLoginPage() {
  const { isAuthenticated, authNotice, login } = useAdminAuth()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [loginError, setLoginError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isSubmittingRef = useRef(false)

  if (isAuthenticated && !isSubmitting) {
    return <Navigate to="/admin/products" replace />
  }

  function handle_password_change(event) {
    setPassword(event.target.value)
    setPasswordError('')
  }

  async function handle_login_submit(event) {
    event.preventDefault()
    // 상태 반영 전에 Enter를 연속으로 눌러도 한 번만 요청한다.
    if (isSubmittingRef.current) {
      return
    }
    setLoginError('')

    // 앞뒤 공백을 제거하지 않고 입력값 그대로 검사·전송한다.
    if (password === '') {
      setPasswordError('비밀번호를 입력해 주세요.')
      return
    }

    isSubmittingRef.current = true
    setIsSubmitting(true)
    try {
      await login(password)
      setPassword('')
      navigate('/admin/products', { replace: true })
    } catch (error) {
      setLoginError(get_login_error_message(error))
      setIsSubmitting(false)
    } finally {
      isSubmittingRef.current = false
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <p className="auth-card__brand">카페 원두 주문</p>
        <h1 className="auth-card__title">관리자 로그인</h1>

        {authNotice && !loginError && (
          <p className="form-notice" role="status">
            {authNotice}
          </p>
        )}

        <form className="auth-card__form" noValidate onSubmit={handle_login_submit}>
          <InputField
            id="admin-password"
            label="비밀번호"
            type="password"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={handle_password_change}
            error={passwordError}
            aria-describedby={loginError ? loginErrorId : undefined}
            readOnly={isSubmitting}
          />

          {loginError && (
            <p id={loginErrorId} className="form-alert" role="alert">
              <span className="form-alert__icon" aria-hidden="true">
                !
              </span>
              <span>
                <span className="visually-hidden">오류: </span>
                {loginError}
              </span>
            </p>
          )}

          <Button type="submit" isLoading={isSubmitting} className="auth-card__submit">
            {isSubmitting ? '로그인 중' : '로그인'}
          </Button>
        </form>

        <Link to="/customer" className="auth-card__back">
          ← 고객 화면으로 돌아가기
        </Link>
      </div>
    </main>
  )
}

export default AdminLoginPage
