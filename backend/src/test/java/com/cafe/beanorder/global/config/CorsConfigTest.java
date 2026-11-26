package com.cafe.beanorder.global.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cafe.beanorder.global.support.CommonModuleTestController;

@SpringBootTest
@ActiveProfiles("test")
@Import(CommonModuleTestController.class)
class CorsConfigTest {

	private static final String ITEMS_URL = "/api/test-support/items";
	private static final String VITE_ORIGIN = "http://localhost:5173";
	private static final String VITE_LOOPBACK_ORIGIN = "http://127.0.0.1:5173";
	private static final String UNKNOWN_ORIGIN = "http://evil.example.com";

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void set_up() {
		// CORS 는 Spring Security 의 CorsFilter 가 처리하므로 실제 보안 필터 체인을 포함한다.
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	@Test
	void allows_actual_request_from_vite_origin() throws Exception {
		mockMvc.perform(get(ITEMS_URL).header(HttpHeaders.ORIGIN, VITE_ORIGIN))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VITE_ORIGIN))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void allows_preflight_from_both_default_origins() throws Exception {
		for (String origin : List.of(VITE_ORIGIN, VITE_LOOPBACK_ORIGIN)) {
			mockMvc.perform(options(ITEMS_URL)
					.header(HttpHeaders.ORIGIN, origin)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("content-type")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		}
	}

	@Test
	void allows_preflight_for_delete() throws Exception {
		mockMvc.perform(options(ITEMS_URL + "/1")
				.header(HttpHeaders.ORIGIN, VITE_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VITE_ORIGIN));
	}

	@Test
	void rejects_preflight_from_unknown_origin() throws Exception {
		mockMvc.perform(options(ITEMS_URL)
				.header(HttpHeaders.ORIGIN, UNKNOWN_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void does_not_add_cors_headers_for_unknown_origin_actual_request() throws Exception {
		mockMvc.perform(get(ITEMS_URL).header(HttpHeaders.ORIGIN, UNKNOWN_ORIGIN))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void rejects_preflight_for_unlisted_method_or_header() throws Exception {
		mockMvc.perform(options(ITEMS_URL)
				.header(HttpHeaders.ORIGIN, VITE_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

		mockMvc.perform(options(ITEMS_URL)
				.header(HttpHeaders.ORIGIN, VITE_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-custom-header"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
	}

	@Test
	void rejects_wildcard_origin_setting() {
		assertThatThrownBy(() -> new CorsProperties(List.of("*")))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CorsProperties(List.of("https://*.vercel.app")))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
