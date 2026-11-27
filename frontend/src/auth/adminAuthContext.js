import { createContext, useContext } from 'react'

export const AdminAuthContext = createContext(null)

/**
 * 관리자 인증 상태를 읽는다. AdminAuthProvider 안에서만 사용한다.
 * { isAuthenticated, authNotice, login, logout }
 */
export function useAdminAuth() {
  const adminAuth = useContext(AdminAuthContext)
  if (!adminAuth) {
    throw new Error('useAdminAuth는 AdminAuthProvider 안에서만 사용할 수 있습니다.')
  }
  return adminAuth
}
