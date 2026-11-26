package com.cafe.beanorder.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * React 개발 서버 등 허용된 Origin 에서 /api/** 를 호출할 수 있도록 하는 CORS 설정.
 * CORS 는 브라우저의 교차 출처 호출 허용 여부일 뿐, 인증·권한 검사가 아니다.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

	static final String[] ALLOWED_METHODS = { "GET", "POST", "PUT", "DELETE", "OPTIONS" };
	static final long PREFLIGHT_MAX_AGE_SECONDS = 3600;

	private final CorsProperties corsProperties;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
			.allowedMethods(ALLOWED_METHODS)
			.allowedHeaders(HttpHeaders.CONTENT_TYPE)
			.allowCredentials(false)
			.maxAge(PREFLIGHT_MAX_AGE_SECONDS);
	}

}
