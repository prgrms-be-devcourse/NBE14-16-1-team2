package com.cafe.beanorder.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cafe.beanorder.global.security.AdminTokenAuthenticationFilter;
import com.cafe.beanorder.global.support.AdminTestController;
import com.cafe.beanorder.global.support.CommonModuleTestController;
import com.cafe.beanorder.support.MutableTestClock;

import tools.jackson.databind.json.JsonMapper;

/**
 * 관리자 로그인·토큰 검사 통합 테스트. 실제 Spring Security 필터 체인(springSecurity())을 포함해 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({ AdminTestController.class, CommonModuleTestController.class })
@ExtendWith(OutputCaptureExtension.class)
class AdminAuthIntegrationTest {

	/** application-test.properties 의 테스트 전용 값 */
	private static final String TEST_PASSWORD = "test-only-admin-password";

	private static final String LOGIN_URL = "/api/admin/login";
	private static final String ADMIN_ITEMS_URL = "/api/admin/test-support/items";
	private static final String PUBLIC_ITEMS_URL = "/api/test-support/items";
	private static final String VITE_ORIGIN = "http://localhost:5173";
	private static final String UNKNOWN_ORIGIN = "http://evil.example.com";
	private static final String TOKEN_FORMAT = "^[A-Za-z0-9_-]{43}$";

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 11, 27, 9, 0, 0);

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		MutableTestClock mutableTestClock() {
			return new MutableTestClock(START_TIME.atZone(SEOUL).toInstant(), SEOUL);
		}

	}

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private MutableTestClock mutableTestClock;

	@Autowired
	private JsonMapper jsonMapper;

	private MockMvc mockMvc;

	@BeforeEach
	void set_up() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
		mutableTestClock.set_instant(START_TIME.atZone(SEOUL).toInstant());
		AdminTestController.HANDLER_CALLS.set(0);
	}

	// ===== 1·10. 로그인 성공 =====

	@Test
	void login_with_correct_password_returns_token_without_wrapper() throws Exception {
		String body = mockMvc.perform(login_request(TEST_PASSWORD))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.token").isString())
			.andExpect(jsonPath("$.success").doesNotExist())
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(TEST_PASSWORD);
		assertThat(token_of(body)).matches(TOKEN_FORMAT);
	}

	@Test
	void login_does_not_require_token_even_with_invalid_authorization_header() throws Exception {
		mockMvc.perform(login_request(TEST_PASSWORD).header(HttpHeaders.AUTHORIZATION, "Bearer not-issued"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isString());
	}

	// ===== 2. 비밀번호 불일치 =====

	@Test
	void login_with_wrong_password_returns_401() throws Exception {
		for (String wrongPassword : List.of("wrong-password", TEST_PASSWORD.toUpperCase(), " " + TEST_PASSWORD,
				TEST_PASSWORD + " ", TEST_PASSWORD.substring(1))) {
			expect_unauthorized(mockMvc.perform(login_request(wrongPassword)))
				.andExpect(jsonPath("$.token").doesNotExist());
		}
	}

	// ===== 3. 입력 오류 =====

	@Test
	void login_with_missing_or_blank_password_returns_400() throws Exception {
		for (String json : List.of("{}", "{\"password\":null}", "{\"password\":\"\"}", "{\"password\":\"   \"}",
				"{\"password\":\"\\t\\n\"}")) {
			expect_invalid_input(mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(json)));
		}
	}

	@Test
	void login_with_non_string_password_returns_400() throws Exception {
		for (String json : List.of("{\"password\":123}", "{\"password\":true}", "{\"password\":[\"a\"]}",
				"{\"password\":{\"value\":\"a\"}}", "{\"password\":1.5}")) {
			expect_invalid_input(mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(json)));
		}
	}

	@Test
	void login_with_malformed_or_missing_body_returns_400() throws Exception {
		for (String json : List.of("{", "{\"password\":}", "null", "", "\"" + TEST_PASSWORD + "\"")) {
			expect_invalid_input(mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(json)));
		}
	}

	@Test
	void login_keeps_existing_415_for_unsupported_content_type() throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.TEXT_PLAIN).content(TEST_PASSWORD))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	// ===== 5. 인증 없이 관리자 접근 =====

	@Test
	void admin_api_without_authorization_header_returns_401() throws Exception {
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL)))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
		expect_unauthorized(mockMvc.perform(post(ADMIN_ITEMS_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemName\":\"원두\",\"quantity\":1}")));
		assertThat(AdminTestController.HANDLER_CALLS.get()).isZero();
	}

	@Test
	void every_admin_path_except_exact_post_login_requires_token() throws Exception {
		expect_unauthorized(mockMvc.perform(get("/api/admin")));
		expect_unauthorized(mockMvc.perform(get("/api/admin/")));
		expect_unauthorized(mockMvc.perform(get("/api/admin/not-exists")));
		expect_unauthorized(mockMvc.perform(get(LOGIN_URL)));
		expect_unauthorized(mockMvc.perform(post(LOGIN_URL + "/extra").contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"" + TEST_PASSWORD + "\"}")));
		expect_unauthorized(mockMvc.perform(post(LOGIN_URL + "/").contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"" + TEST_PASSWORD + "\"}")));
	}

	@Test
	void plain_options_request_is_not_exempt_from_authentication() throws Exception {
		// Origin·Access-Control-Request-Method 가 없는 OPTIONS 는 preflight 가 아니므로 인증 대상이다.
		expect_unauthorized(mockMvc.perform(options(ADMIN_ITEMS_URL)));
		assertThat(AdminTestController.HANDLER_CALLS.get()).isZero();
	}

	// ===== 6. 잘못된 토큰·헤더 =====

	@Test
	void admin_api_with_invalid_authorization_returns_401() throws Exception {
		String validToken = login_and_get_token();
		List<String> invalidHeaders = List.of(
				"",
				"Bearer",
				"Bearer ",
				"Bearer  " + validToken,
				"Bearer " + validToken + " ",
				"Basic " + validToken,
				"Token " + validToken,
				validToken,
				"Bearer " + validToken + "x",
				"Bearer " + validToken.substring(1),
				"Bearer " + "A".repeat(43),
				"Bearer " + TEST_PASSWORD,
				"Bearer " + validToken + "," + validToken);
		for (String invalidHeader : invalidHeaders) {
			expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, invalidHeader)));
		}
		assertThat(AdminTestController.HANDLER_CALLS.get()).isZero();
	}

	@Test
	void token_is_accepted_only_from_authorization_header() throws Exception {
		String token = login_and_get_token();
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).param("token", token)));
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).param("access_token", token)));
		expect_unauthorized(mockMvc.perform(post(ADMIN_ITEMS_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + token + "\",\"itemName\":\"원두\",\"quantity\":1}")));
		// Authorization 헤더가 두 개면 거부
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token, "Bearer " + token)));
	}

	// ===== 7. 유효 토큰 =====

	@Test
	void admin_api_with_valid_token_is_allowed() throws Exception {
		String token = login_and_get_token();
		mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].itemName").value("관리자 전용 원두"));
		// 인증 방식 이름은 대소문자 구분 없음 (RFC 7235)
		mockMvc.perform(get(ADMIN_ITEMS_URL + "/1").header(HttpHeaders.AUTHORIZATION, "bearer " + token))
			.andExpect(status().isOk());
		assertThat(AdminTestController.HANDLER_CALLS.get()).isEqualTo(2);
	}

	// ===== 8. 만료 경계값 =====

	@Test
	void token_expires_exactly_after_ttl() throws Exception {
		String token = login_and_get_token();

		mutableTestClock.advance(Duration.ofHours(1).minusNanos(1));
		mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());

		mutableTestClock.advance(Duration.ofNanos(1));
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));

		// 시계를 되돌려도 만료 시 제거된 토큰은 다시 살아나지 않는다.
		mutableTestClock.set_instant(START_TIME.atZone(SEOUL).toInstant());
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
	}

	// ===== 9. 로그인 두 번 =====

	@Test
	void second_login_does_not_invalidate_first_token() throws Exception {
		String firstToken = login_and_get_token();
		mutableTestClock.advance(Duration.ofMinutes(30));
		String secondToken = login_and_get_token();
		assertThat(secondToken).isNotEqualTo(firstToken);

		for (String token : List.of(firstToken, secondToken)) {
			mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		}

		// 각자의 만료 시각을 따른다: 첫 토큰 만료 시점에 두 번째 토큰은 아직 유효
		mutableTestClock.advance(Duration.ofMinutes(30));
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)));
		mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
			.andExpect(status().isOk());
	}

	// ===== 11. 고객(공개) API =====

	@Test
	void public_api_does_not_require_admin_token() throws Exception {
		mockMvc.perform(get(PUBLIC_ITEMS_URL)).andExpect(status().isOk());
		// 잘못된 토큰을 붙여도 공개 API 는 막지 않는다.
		mockMvc.perform(get(PUBLIC_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer not-issued"))
			.andExpect(status().isOk());
	}

	@Test
	void public_paths_keep_existing_404_and_405() throws Exception {
		// /api/products 는 상품 API 구현으로 생긴 경로라, 없는 공개 경로로 404 유지를 확인한다.
		mockMvc.perform(get("/api/not-existing-path"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
		mockMvc.perform(patch(PUBLIC_ITEMS_URL))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	// ===== 12. 인증 후 업무 오류의 원래 상태 유지 =====

	@Test
	void business_errors_after_authentication_keep_original_status() throws Exception {
		String authorization = "Bearer " + login_and_get_token();

		mockMvc.perform(post(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON).content("{\"itemName\":\"\",\"quantity\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(get(ADMIN_ITEMS_URL + "/404").header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
		mockMvc.perform(get(ADMIN_ITEMS_URL + "/409").header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ORDER_STATUS_CONFLICT"));
		mockMvc.perform(get(ADMIN_ITEMS_URL + "/500").header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
		mockMvc.perform(get("/api/admin/not-exists").header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
		mockMvc.perform(patch(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	// ===== 13·14·15. CORS =====

	@Test
	void preflight_with_authorization_header_is_allowed_without_token() throws Exception {
		for (String url : List.of(ADMIN_ITEMS_URL, ADMIN_ITEMS_URL + "/1", LOGIN_URL)) {
			mockMvc.perform(options(url)
					.header(HttpHeaders.ORIGIN, VITE_ORIGIN)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VITE_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("content-type")))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
		}
		assertThat(AdminTestController.HANDLER_CALLS.get()).isZero();
	}

	@Test
	void unauthorized_responses_include_cors_headers_for_allowed_origin() throws Exception {
		expect_unauthorized(mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.ORIGIN, VITE_ORIGIN)))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VITE_ORIGIN));
		expect_unauthorized(mockMvc.perform(login_request("wrong-password").header(HttpHeaders.ORIGIN, VITE_ORIGIN)))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VITE_ORIGIN));
	}

	@Test
	void unknown_origin_is_rejected_by_cors_before_authentication() throws Exception {
		String token = login_and_get_token();
		// CORS 거부(403)는 토큰 유효 여부와 무관하며, 관리자 로직은 실행되지 않는다.
		mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.ORIGIN, UNKNOWN_ORIGIN)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		mockMvc.perform(options(ADMIN_ITEMS_URL)
				.header(HttpHeaders.ORIGIN, UNKNOWN_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertThat(AdminTestController.HANDLER_CALLS.get()).isZero();
	}

	// ===== 보안 구성 점검 =====

	@Test
	void token_filter_is_registered_once_and_no_default_user_exists(ApplicationContext applicationContext) {
		assertThat(applicationContext.getBeansOfType(AdminTokenAuthenticationFilter.class)).isEmpty();
		assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).isEmpty();

		FilterChainProxy filterChainProxy = applicationContext.getBean(FilterChainProxy.class);
		long tokenFilterCount = filterChainProxy.getFilterChains().stream()
			.flatMap(chain -> chain.getFilters().stream())
			.filter(AdminTokenAuthenticationFilter.class::isInstance)
			.count();
		assertThat(tokenFilterCount).isEqualTo(1);
	}

	@Test
	void does_not_log_password_or_token(CapturedOutput output) throws Exception {
		String token = login_and_get_token();
		mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
		mockMvc.perform(get(ADMIN_ITEMS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
			.andExpect(status().isUnauthorized());
		expect_unauthorized(mockMvc.perform(login_request("wrong-password")));

		assertThat(output.getAll()).doesNotContain(TEST_PASSWORD).doesNotContain(token)
			.doesNotContain("wrong-password").doesNotContain("Using generated security password");
	}

	// ===== 도우미 =====

	private MockHttpServletRequestBuilder login_request(String password) throws Exception {
		return post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
			.content(jsonMapper.writeValueAsString(new LoginBody(password)));
	}

	private record LoginBody(String password) {
	}

	private String login_and_get_token() throws Exception {
		String body = mockMvc.perform(login_request(TEST_PASSWORD))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return token_of(body);
	}

	private String token_of(String body) {
		return jsonMapper.readTree(body).get("token").asString();
	}

	private static ResultActions expect_unauthorized(ResultActions resultActions) throws Exception {
		return resultActions
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.message").value("관리자 인증에 실패했습니다."));
	}

	private static ResultActions expect_invalid_input(ResultActions resultActions) throws Exception {
		return resultActions
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

}
