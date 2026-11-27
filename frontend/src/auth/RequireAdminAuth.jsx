import { Navigate, Outlet } from 'react-router'
import { useAdminAuth } from './adminAuthContext.js'

/**
 * 관리자 화면 보호 라우트. 토큰이 없으면 /admin/login으로 replace 이동한다.
 * 화면 접근 제어는 사용자 편의일 뿐이고, 실제 권한 검사는 서버가 한다.
 * /admin/login은 이 라우트 밖에 두어야 한다. (안에 두면 무한 이동)
 */
function RequireAdminAuth() {
  const { isAuthenticated } = useAdminAuth()

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />
  }
  return <Outlet />
}

export default RequireAdminAuth
