/*
 * 관리자 토큰 저장소 (저장·조회·삭제는 이 파일에서만 한다)
 *
 * 이번 구현의 선택: sessionStorage 사용
 * - 브라우저 탭을 닫으면 사라지고, 새로고침에는 유지된다.
 * - 팀 전체의 최종 보안 정책으로 확정된 것은 아니다. (README 7-5 참고)
 * - JavaScript로 읽을 수 있어 XSS에 안전한 저장소가 아니다.
 *
 * 토큰은 서버가 발급한 불투명 문자열(JWT 아님)이므로 디코딩하거나 만료 시각을 추측하지 않는다.
 * 토큰 값을 로그에 출력하지 않는다.
 */
const adminTokenKey = 'adminToken'

// sessionStorage를 쓸 수 없는 환경(일부 시크릿 모드 등)에서는 현재 탭 메모리에만 보관한다.
let memoryToken = null

function get_session_storage() {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

export function read_admin_token() {
  const storage = get_session_storage()
  if (!storage) {
    return memoryToken
  }
  try {
    const storedToken = storage.getItem(adminTokenKey)
    return storedToken ? storedToken : null
  } catch {
    return memoryToken
  }
}

export function save_admin_token(token) {
  memoryToken = token
  const storage = get_session_storage()
  try {
    storage?.setItem(adminTokenKey, token)
  } catch {
    // 저장 실패 시 memoryToken으로만 유지 (새로고침하면 다시 로그인 필요)
  }
}

export function clear_admin_token() {
  memoryToken = null
  const storage = get_session_storage()
  try {
    storage?.removeItem(adminTokenKey)
  } catch {
    // 삭제 실패는 무시 (memoryToken은 이미 비움)
  }
}
