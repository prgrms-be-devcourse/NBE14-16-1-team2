/*
 * 관리자 API 요청 모듈
 * - Authorization: Bearer {token} 은 request_admin 에서만 붙인다. (로그인·공개 API에는 붙이지 않음)
 * - 관리자 요청이 401이면 등록된 처리 함수(AdminAuthProvider)가 토큰과 로그인 상태를 지운다.
 * - 400·404·409·500 등 다른 오류는 로그인 상태를 건드리지 않고 그대로 ApiError로 던진다.
 *
 * 사용 예 (후속 기능 담당자용, 상품·주문 화면에서):
 *   import { request_admin } from '../../api/adminApi.js'
 *
 *   const orders = await request_admin('/api/admin/orders')
 *   await request_admin(`/api/admin/products/${productId}`, { method: 'PUT', body: productForm })
 *
 *   catch (error) {
 *     if (error.kind === 'unauthorized') return // 로그인 화면으로 이동 중이므로 따로 표시하지 않음
 *     setErrorMessage(error.message)            // 그 밖의 오류는 화면에 표시
 *   }
 */
import { clear_admin_token, read_admin_token } from '../auth/adminTokenStorage.js'
import { ApiError, request_api } from './apiClient.js'

let handleAdminUnauthorized = null

// AdminAuthProvider가 등록한다. 401을 받았을 때 (요청에 쓴 토큰)을 넘겨 호출한다.
export function set_admin_unauthorized_handler(handler) {
  handleAdminUnauthorized = handler
}

function notify_admin_unauthorized(usedToken) {
  if (handleAdminUnauthorized) {
    handleAdminUnauthorized(usedToken)
    return
  }
  // 처리 함수가 없을 때도 토큰은 지운다 (같은 토큰일 때만)
  if (read_admin_token() === usedToken) {
    clear_admin_token()
  }
}

/**
 * 관리자 로그인. 성공하면 토큰 문자열을 돌려준다. (저장은 AdminAuthProvider가 한다)
 * 비밀번호는 입력값 그대로 보내며 공백을 제거하거나 저장·기록하지 않는다.
 * 로그인 실패의 401은 kind 'http'로 던진다. (관리자 요청의 401인 'unauthorized'와 구분)
 */
export async function login_admin(password) {
  const responseData = await request_api('/api/admin/login', { method: 'POST', body: { password } })
  const token = responseData?.token
  if (typeof token !== 'string' || token.length === 0) {
    throw new ApiError({ kind: 'invalid_response', status: 200, message: '로그인 응답에 토큰이 없습니다.' })
  }
  return token
}

/** 관리자 API 요청. 옵션은 request_api와 같다. */
export async function request_admin(path, { headers = {}, ...options } = {}) {
  const usedToken = read_admin_token()
  if (!usedToken) {
    notify_admin_unauthorized(null)
    throw new ApiError({ kind: 'unauthorized', status: 401, message: '관리자 로그인이 필요합니다.' })
  }

  try {
    return await request_api(path, { ...options, headers: { ...headers, Authorization: `Bearer ${usedToken}` } })
  } catch (error) {
    if (error instanceof ApiError && error.kind === 'http' && error.status === 401) {
      notify_admin_unauthorized(usedToken)
      throw new ApiError({
        kind: 'unauthorized',
        status: 401,
        code: error.code,
        message: '로그인이 만료되었거나 인증이 유효하지 않습니다.',
      })
    }
    throw error
  }
}
