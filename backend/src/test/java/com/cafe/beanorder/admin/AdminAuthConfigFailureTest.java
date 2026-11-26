package com.cafe.beanorder.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.cafe.beanorder.BeanOrderApplication;

/**
 * 관리자 인증 설정이 잘못되면 서버가 시작되지 않는지 실제 SpringApplication 으로 확인한다.
 * 명령줄 인자(최우선)로 test 프로필 값을 덮어쓰고, 다른 테스트와 겹치지 않도록 별도 인메모리 DB 를 쓴다.
 */
@ExtendWith(OutputCaptureExtension.class)
class AdminAuthConfigFailureTest {

	private static final String SECRET_LIKE_PASSWORD = "config-failure-test-password";

	@Test
	void fails_to_start_when_admin_password_is_missing() {
		Throwable failure = start_with("--app.admin.password=");
		assertThat(messages_of(failure)).anyMatch(message -> message.contains("ADMIN_PASSWORD"));
	}

	@Test
	void fails_to_start_when_admin_password_is_blank() {
		Throwable failure = start_with("--app.admin.password=   ");
		assertThat(messages_of(failure)).anyMatch(message -> message.contains("ADMIN_PASSWORD"));
	}

	@Test
	void fails_to_start_when_token_ttl_is_not_positive(CapturedOutput output) {
		for (String ttl : List.of("0s", "-1m")) {
			Throwable failure = start_with("--app.admin.password=" + SECRET_LIKE_PASSWORD, "--app.admin.token-ttl=" + ttl);
			assertThat(messages_of(failure)).anyMatch(message -> message.contains("token-ttl"));
			assertThat(messages_of(failure)).noneMatch(message -> message.contains(SECRET_LIKE_PASSWORD));
		}
		assertThat(output.getAll()).doesNotContain(SECRET_LIKE_PASSWORD);
	}

	@Test
	void fails_to_start_when_max_active_tokens_is_not_positive() {
		Throwable failure = start_with("--app.admin.password=" + SECRET_LIKE_PASSWORD, "--app.admin.max-active-tokens=0");
		assertThat(messages_of(failure)).anyMatch(message -> message.contains("max-active-tokens"));
	}

	@Test
	void properties_to_string_hides_password() {
		AdminAuthProperties properties = new AdminAuthProperties(SECRET_LIKE_PASSWORD, Duration.ofHours(1), 100);
		assertThat(properties.toString()).doesNotContain(SECRET_LIKE_PASSWORD).contains("[PROTECTED]");
		assertThat(new AdminLoginRequest(SECRET_LIKE_PASSWORD).toString()).doesNotContain(SECRET_LIKE_PASSWORD);
	}

	private static Throwable start_with(String... overrides) {
		List<String> args = new ArrayList<>(List.of(
				"--server.port=0",
				"--spring.datasource.url=jdbc:h2:mem:admin-config-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"));
		args.addAll(List.of(overrides));
		Throwable failure = catchThrowable(() -> new SpringApplicationBuilder(BeanOrderApplication.class)
			.profiles("test")
			.run(args.toArray(String[]::new))
			.close());
		assertThat(failure).as("잘못된 관리자 설정으로는 서버가 시작되면 안 된다").isNotNull();
		return failure;
	}

	private static List<String> messages_of(Throwable failure) {
		List<String> messages = new ArrayList<>();
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			messages.add(String.valueOf(cause.getMessage()));
		}
		return messages;
	}

}
