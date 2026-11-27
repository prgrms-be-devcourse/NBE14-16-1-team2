/*
 * 공통 API 요청 모듈
 * - 서버 주소는 VITE_API_BASE_URL 환경변수에서만 읽는다. (소스에 배포 주소를 넣지 않는다)
 * - 응답은 래퍼 없이 실제 데이터, 오류는 { code, message } 형식 (README 7-2)
 * - 이 함수는 인증 헤더를 붙이지 않는다. 관리자 API는 adminApi.js의 request_admin을 사용한다.
 * - 요청·응답 본문, 토큰, Authorization 헤더를 로그에 남기지 않는다.
 */

/**
 * API 요청 실패를 나타내는 오류
 * kind
 *  - 'config': VITE_API_BASE_URL 미설정 (개발자 설정 문제)
 *  - 'network': 서버에 연결하지 못함 (서버 꺼짐·CORS 차단·네트워크 끊김)
 *  - 'http': 서버가 오류 상태 코드로 응답함 (status·code·message 확인)
 *  - 'invalid_response': 성공 응답이지만 기대한 형식이 아님
 *  - 'unauthorized': 관리자 요청이 401을 받아 로그인이 해제됨 (adminApi.js)
 */
export class ApiError extends Error {
  constructor({ kind, message, status = null, code = null }) {
    super(message)
    this.name = 'ApiError'
    this.kind = kind
    this.status = status
    this.code = code
  }
}

let hasReportedMissingBaseUrl = false

// 서버 주소를 읽는다. 없으면 빈 주소(현재 화면 주소)로 잘못 요청하지 않도록 바로 실패시킨다.
export function get_api_base_url() {
  const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').trim()
  if (!baseUrl) {
    if (!hasReportedMissingBaseUrl) {
      hasReportedMissingBaseUrl = true
      console.error(
        '[API] VITE_API_BASE_URL 환경변수가 설정되지 않았습니다. ' +
          'frontend/.env.example을 참고해 frontend/.env.local에 서버 주소를 넣고 개발 서버를 다시 시작하세요.',
      )
    }
    throw new ApiError({ kind: 'config', message: '서버 주소가 설정되지 않았습니다.' })
  }
  return baseUrl
}

// 서버 주소와 경로를 슬래시 하나로 연결한다. 예) "http://a.com/" + "/api/x" → "http://a.com/api/x"
export function build_api_url(path) {
  const baseUrl = get_api_base_url().replace(/\/+$/, '')
  const normalizedPath = String(path).replace(/^\/+/, '')
  return `${baseUrl}/${normalizedPath}`
}

// 응답 본문을 읽는다. 빈 본문·JSON이 아닌 본문이어도 예외를 던지지 않는다.
async function read_response_body(response) {
  let bodyText = ''
  try {
    bodyText = await response.text()
  } catch {
    return { isEmpty: true, isJson: false, data: null }
  }
  if (!bodyText.trim()) {
    return { isEmpty: true, isJson: false, data: null }
  }
  try {
    return { isEmpty: false, isJson: true, data: JSON.parse(bodyText) }
  } catch {
    return { isEmpty: false, isJson: false, data: null }
  }
}

function is_error_body(data) {
  return data !== null && typeof data === 'object' && typeof data.message === 'string'
}

/**
 * JSON API 요청 (인증 헤더 없음: 공개 API·로그인 요청용)
 * options.body: 객체를 넘기면 JSON으로 변환해 보낸다.
 * 성공하면 응답 데이터(빈 본문이면 null)를 돌려주고, 실패하면 ApiError를 던진다.
 * 오류 본문이 비었거나 JSON이 아니어도 원래 status는 ApiError.status에 그대로 남는다.
 */
export async function request_api(path, { method = 'GET', body, headers = {}, signal } = {}) {
  const requestUrl = build_api_url(path)
  const requestHeaders = { Accept: 'application/json', ...headers }
  const requestInit = { method, headers: requestHeaders, signal }
  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json'
    requestInit.body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(requestUrl, requestInit)
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw error
    }
    throw new ApiError({ kind: 'network', message: '서버에 연결할 수 없습니다.' })
  }

  const responseBody = await read_response_body(response)

  if (!response.ok) {
    const errorBody = responseBody.isJson && is_error_body(responseBody.data) ? responseBody.data : null
    throw new ApiError({
      kind: 'http',
      status: response.status,
      code: typeof errorBody?.code === 'string' ? errorBody.code : null,
      message: errorBody?.message ?? `요청이 실패했습니다. (HTTP ${response.status})`,
    })
  }

  if (responseBody.isEmpty) {
    return null
  }
  if (!responseBody.isJson) {
    throw new ApiError({
      kind: 'invalid_response',
      status: response.status,
      message: '서버 응답 형식이 올바르지 않습니다.',
    })
  }
  return responseBody.data
}
