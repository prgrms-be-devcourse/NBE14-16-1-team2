import { useCallback, useEffect, useMemo, useState } from 'react'
import { login_admin, set_admin_unauthorized_handler } from '../api/adminApi.js'
import { AdminAuthContext } from './adminAuthContext.js'
import { clear_admin_token, read_admin_token, save_admin_token } from './adminTokenStorage.js'

const expiredSessionMessage = '로그인이 만료되었거나 인증이 유효하지 않습니다. 다시 로그인해 주세요.'

/**
 * 관리자 인증 상태 (React 상태 ↔ sessionStorage 동기화)
 * - 새로고침하면 저장된 토큰으로 상태를 초기화한다.
 * - 토큰이 있다는 것은 "로그인한 적 있음"일 뿐 서버 검증이 끝났다는 뜻이 아니다.
 *   만료·무효는 실제 관리자 API의 401 응답으로 알게 되며, 그때 토큰과 상태를 함께 지운다.
 */
function AdminAuthProvider({ children }) {
  const [token, setToken] = useState(() => read_admin_token())
  const [authNotice, setAuthNotice] = useState('')

  // 관리자 요청 401 처리. 여러 요청의 401이 겹쳐도 처음 한 번만 처리한다.
  // (요청에 쓴 토큰이 이미 지워졌거나 새 로그인으로 바뀌었다면 무시)
  const handle_admin_unauthorized = useCallback((usedToken) => {
    const currentToken = read_admin_token()
    if (usedToken === null) {
      if (currentToken === null) {
        setToken(null)
      }
      return
    }
    if (currentToken !== usedToken) {
      return
    }
    clear_admin_token()
    setToken(null)
    setAuthNotice(expiredSessionMessage)
  }, [])

  useEffect(() => {
    set_admin_unauthorized_handler(handle_admin_unauthorized)
    return () => set_admin_unauthorized_handler(null)
  }, [handle_admin_unauthorized])

  const login = useCallback(async (password) => {
    const issuedToken = await login_admin(password)
    save_admin_token(issuedToken)
    setToken(issuedToken)
    setAuthNotice('')
  }, [])

  // 브라우저의 토큰만 지운다. 서버에 발급된 토큰은 만료 시각까지 유효하다. (로그아웃 API 없음)
  const logout = useCallback(() => {
    clear_admin_token()
    setToken(null)
    setAuthNotice('')
  }, [])

  const adminAuth = useMemo(
    () => ({ isAuthenticated: Boolean(token), authNotice, login, logout }),
    [token, authNotice, login, logout],
  )

  return <AdminAuthContext.Provider value={adminAuth}>{children}</AdminAuthContext.Provider>
}

export default AdminAuthProvider
