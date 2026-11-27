package com.cafe.beanorder.global.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cafe.beanorder.admin.AdminTokenStore;

/**
 * Authorization: Bearer {token} 헤더의 관리자 토큰을 확인해, 유효하면 이 요청에 한해 관리자 인증을 설정한다.
 *
 * <ul>
 *   <li>헤더가 없거나 형식이 틀리거나 토큰이 무효이면 아무것도 설정하지 않는다.
 *       보호 경로라면 이후 인가 단계에서 401 이 되고, 공개 경로는 그대로 통과한다.</li>
 *   <li>토큰은 헤더로만 받는다. (URL·쿼리·본문 사용 안 함)</li>
 *   <li>보안 설정에서 직접 생성해 필터 체인에만 넣는다. 빈으로 등록하지 않아 서블릿 필터로 중복 등록되지 않는다.</li>
 *   <li>토큰과 헤더 값은 로그에 남기지 않는다.</li>
 * </ul>
 */
public class AdminTokenAuthenticationFilter extends OncePerRequestFilter {

	static final String ADMIN_ROLE = "ADMIN";

	/** 인증 방식 이름은 대소문자를 구분하지 않고(RFC 7235), 토큰은 URL-safe Base64 문자만 허용한다. */
	private static final Pattern BEARER_PATTERN = Pattern.compile("^(?i:Bearer) ([A-Za-z0-9_-]+)$");

	private final AdminTokenStore adminTokenStore;
	private final SecurityContextHolderStrategy securityContextHolderStrategy =
			SecurityContextHolder.getContextHolderStrategy();

	public AdminTokenAuthenticationFilter(AdminTokenStore adminTokenStore) {
		this.adminTokenStore = adminTokenStore;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = extract_bearer_token(request);
		if (token != null && adminTokenStore.is_valid(token)) {
			UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
					"admin", null, AuthorityUtils.createAuthorityList("ROLE_" + ADMIN_ROLE));
			SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();
			securityContext.setAuthentication(authentication);
			securityContextHolderStrategy.setContext(securityContext);
		}
		filterChain.doFilter(request, response);
	}

	/** Authorization 헤더가 정확히 하나이고 "Bearer {token}" 형식일 때만 토큰을 꺼낸다. */
	private static String extract_bearer_token(HttpServletRequest request) {
		List<String> headerValues = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
		if (headerValues.size() != 1) {
			return null;
		}
		Matcher matcher = BEARER_PATTERN.matcher(headerValues.get(0));
		return matcher.matches() ? matcher.group(1) : null;
	}

}
