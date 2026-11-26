package com.cafe.beanorder.admin;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 인증 설정 (app.admin.*).
 *
 * <ul>
 *   <li>password: 환경변수 ADMIN_PASSWORD 에서 읽는다. 기본값이 없으며, 누락·빈 값이면 서버 시작을 중단한다.</li>
 *   <li>tokenTtl: 토큰 유효 시간. 팀 확정 전 임시 기본값 1시간. 0 이하이면 시작을 중단한다.</li>
 *   <li>maxActiveTokens: 메모리에 동시에 보관할 유효 토큰 최대 개수. 넘으면 가장 오래된 토큰부터 제거한다.</li>
 * </ul>
 *
 * 비밀번호가 로그·오류 메시지에 나오지 않도록 toString 을 재정의하고, 검증 메시지에 값을 넣지 않는다.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminAuthProperties(String password, Duration tokenTtl, Integer maxActiveTokens) {

	public AdminAuthProperties {
		if (password == null || password.isBlank()) {
			throw new IllegalStateException(
					"관리자 비밀번호가 설정되지 않았습니다. 환경변수 ADMIN_PASSWORD 를 설정한 뒤 서버를 시작하세요.");
		}
		if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
			throw new IllegalStateException("app.admin.token-ttl 은 0보다 커야 합니다.");
		}
		if (maxActiveTokens == null || maxActiveTokens <= 0) {
			throw new IllegalStateException("app.admin.max-active-tokens 는 0보다 커야 합니다.");
		}
	}

	@Override
	public String toString() {
		return "AdminAuthProperties[password=[PROTECTED], tokenTtl=" + tokenTtl + ", maxActiveTokens="
				+ maxActiveTokens + "]";
	}

}
