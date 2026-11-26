package com.cafe.beanorder.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 Origin 설정 (app.cors.allowed-origins).
 * 기본값은 application.properties, 운영·배포 주소는 환경변수 CORS_ALLOWED_ORIGINS 로 바꾼다.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();
		if (allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
			throw new IllegalArgumentException("app.cors.allowed-origins 에는 와일드카드(*)를 사용할 수 없습니다.");
		}
	}

}
