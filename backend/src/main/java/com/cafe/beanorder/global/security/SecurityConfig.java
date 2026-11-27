package com.cafe.beanorder.global.security;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.cafe.beanorder.admin.AdminTokenStore;
import com.cafe.beanorder.global.error.ErrorCode;

/**
 * 보안 설정. 관리자 API(/api/admin, /api/admin/**)만 Bearer 토큰으로 보호하고 나머지 경로는 그대로 둔다.
 *
 * <ul>
 *   <li>인증 제외: 정확히 POST /api/admin/login. (/api/admin/login/** 나 다른 메서드는 제외하지 않음)</li>
 *   <li>CORS: CorsConfig 의 CorsConfigurationSource 하나를 CorsFilter 가 사용한다. CorsFilter 가 인증보다 먼저 실행돼
 *       preflight 는 토큰 없이 응답되고 업무 로직까지 가지 않는다. preflight 가 아닌 OPTIONS 는 인증 대상이다.</li>
 *   <li>세션·폼 로그인·HTTP Basic·로그아웃 페이지를 쓰지 않는다. 인증은 요청마다 헤더로만 확인한다(STATELESS).</li>
 *   <li>CSRF: 비활성화. 브라우저가 자동으로 붙이는 쿠키·세션 인증을 쓰지 않고, 클라이언트 코드가 명시적으로 넣는
 *       Authorization 헤더만 인증 수단이라 다른 사이트가 사용자 몰래 인증된 요청을 만들 수 없다.
 *       쿠키 기반 인증을 도입하면 다시 검토해야 한다.</li>
 *   <li>인증 실패는 SecurityErrorResponseWriter 로 401 UNAUTHORIZED code/message JSON 을 쓴다.</li>
 *   <li>업무 오류(400·404·409·500)는 대부분 @RestControllerAdvice 가 같은 요청 안에서 처리한다.
 *       컨테이너 오류 처리(ERROR dispatch)로 넘어가는 경우도 /error 경로로 가고 인증 정보가 요청 속성으로 유지돼
 *       원래 상태가 유지된다(AdminAuthServerTest 로 확인). ERROR dispatch 허용은 오류 페이지 경로가 바뀌어도
 *       오류 응답이 401 로 덮이지 않게 하는 방어 설정이며, 보호 경로의 최초 요청은 REQUEST 단계에서 검사된다.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	static final String ADMIN_LOGIN_PATH = "/api/admin/login";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AdminTokenStore adminTokenStore,
			SecurityErrorResponseWriter securityErrorResponseWriter) throws Exception {
		AuthenticationEntryPoint authenticationEntryPoint = (request, response, authException) -> {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
			securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED);
		};

		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
			.addFilterBefore(new AdminTokenAuthenticationFilter(adminTokenStore), AnonymousAuthenticationFilter.class)
			.authorizeHttpRequests(authorize -> authorize
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers(HttpMethod.POST, ADMIN_LOGIN_PATH).permitAll()
				.requestMatchers("/api/admin", "/api/admin/**").hasRole(AdminTokenAuthenticationFilter.ADMIN_ROLE)
				.anyRequest().permitAll());
		return http.build();
	}

}
