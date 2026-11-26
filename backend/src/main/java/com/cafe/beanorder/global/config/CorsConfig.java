package com.cafe.beanorder.global.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * React 개발 서버 등 허용된 Origin 에서 /api/** 를 호출할 수 있도록 하는 CORS 설정.
 * CORS 는 브라우저의 교차 출처 호출 허용 여부일 뿐, 인증·권한 검사가 아니다.
 *
 * <p>이 CorsConfigurationSource 하나를 Spring Security 의 CorsFilter 가 사용한다(SecurityConfig 의 cors()).
 * 필터 단계에서 처리해야 preflight 가 토큰 검사 전에 응답되고, 401 인증 실패 응답에도 CORS 헤더가 붙는다.
 * Spring MVC 쪽에 별도 CORS 매핑을 두지 않는다.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

	static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
	static final List<String> ALLOWED_HEADERS = List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
	static final long PREFLIGHT_MAX_AGE_SECONDS = 3600;

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(corsProperties.allowedOrigins());
		corsConfiguration.setAllowedMethods(ALLOWED_METHODS);
		corsConfiguration.setAllowedHeaders(ALLOWED_HEADERS);
		corsConfiguration.setAllowCredentials(false);
		corsConfiguration.setMaxAge(PREFLIGHT_MAX_AGE_SECONDS);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", corsConfiguration);
		return source;
	}

}
