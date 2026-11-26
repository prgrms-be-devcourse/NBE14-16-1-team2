package com.cafe.beanorder.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import com.cafe.beanorder.global.support.AdminTestController;

import tools.jackson.databind.json.JsonMapper;

/**
 * 실제 내장 Tomcat 으로 띄워 확인하는 관리자 인증 테스트.
 * MockMvc 는 서블릿 컨테이너의 ERROR dispatch 를 재현하지 않으므로, 인증 후 컨테이너 오류 처리 단계에서
 * 상태 코드가 401 로 바뀌지 않는지 여기서 확인한다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestController.class)
class AdminAuthServerTest {

	private static final String VITE_ORIGIN = "http://localhost:5173";

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void login_and_call_admin_api_through_real_server() throws Exception {
		String token = login();

		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/test-support/items"))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).GET());
		assertThat(response.statusCode()).isEqualTo(200);
	}

	@Test
	void unauthorized_response_from_real_server_has_json_body_and_cors_header() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/test-support/items"))
			.header(HttpHeaders.ORIGIN, VITE_ORIGIN).GET());

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).hasValue(VITE_ORIGIN);
		assertThat(response.headers().firstValue(HttpHeaders.SET_COOKIE)).isEmpty();
		assertThat(jsonMapper.readTree(response.body()).get("code").asString()).isEqualTo("UNAUTHORIZED");
	}

	@Test
	void error_dispatch_after_authentication_keeps_original_status() throws Exception {
		String token = login();

		HttpResponse<String> authenticated = send(HttpRequest.newBuilder(uri("/api/admin/test-support/items/send-error"))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).GET());
		assertThat(authenticated.statusCode()).isEqualTo(409);

		HttpResponse<String> anonymous = send(HttpRequest.newBuilder(uri("/api/admin/test-support/items/send-error")).GET());
		assertThat(anonymous.statusCode()).isEqualTo(401);
	}

	@Test
	void login_does_not_create_session_cookie() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/login"))
			.header(HttpHeaders.CONTENT_TYPE, "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"test-only-admin-password\"}")));
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	private String login() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/login"))
			.header(HttpHeaders.CONTENT_TYPE, "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"test-only-admin-password\"}")));
		assertThat(response.statusCode()).isEqualTo(200);
		return jsonMapper.readTree(response.body()).get("token").asString();
	}

	private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

}
