package com.cafe.beanorder.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.cafe.beanorder.support.MutableTestClock;

/** AdminTokenStore 단위 테스트. 시간은 MutableTestClock 으로 제어한다. */
class AdminTokenStoreTest {

	private static final Instant START = Instant.parse("2026-11-27T00:00:00Z");
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final MutableTestClock clock = new MutableTestClock(START, SEOUL);

	private AdminTokenStore store(Duration ttl, int maxActiveTokens) {
		return new AdminTokenStore(clock, new AdminAuthProperties("test-only-admin-password", ttl, maxActiveTokens));
	}

	@Test
	void issues_unpredictable_url_safe_tokens_of_32_random_bytes() {
		AdminTokenStore store = store(Duration.ofHours(1), 2000);
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			String token = store.issue_token();
			assertThat(token).matches("^[A-Za-z0-9_-]{43}$");
			assertThat(token).doesNotContain("test-only-admin-password");
			tokens.add(token);
		}
		assertThat(tokens).hasSize(1000);
	}

	@Test
	void rejects_null_empty_and_unknown_tokens() {
		AdminTokenStore store = store(Duration.ofHours(1), 10);
		store.issue_token();
		assertThat(store.is_valid(null)).isFalse();
		assertThat(store.is_valid("")).isFalse();
		assertThat(store.is_valid("A".repeat(43))).isFalse();
	}

	@Test
	void token_is_valid_until_just_before_expiry() {
		AdminTokenStore store = store(Duration.ofMinutes(10), 10);
		String token = store.issue_token();

		clock.advance(Duration.ofMinutes(10).minusNanos(1));
		assertThat(store.is_valid(token)).isTrue();

		clock.advance(Duration.ofNanos(1));
		assertThat(store.is_valid(token)).isFalse();
		assertThat(store.stored_token_count()).isZero();
	}

	@Test
	void issuing_removes_expired_tokens() {
		AdminTokenStore store = store(Duration.ofMinutes(10), 10);
		store.issue_token();
		store.issue_token();
		assertThat(store.stored_token_count()).isEqualTo(2);

		clock.advance(Duration.ofMinutes(10));
		String freshToken = store.issue_token();
		assertThat(store.stored_token_count()).isEqualTo(1);
		assertThat(store.is_valid(freshToken)).isTrue();
	}

	@Test
	void keeps_at_most_max_tokens_by_removing_oldest() {
		AdminTokenStore store = store(Duration.ofHours(1), 3);
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			tokens.add(store.issue_token());
			clock.advance(Duration.ofSeconds(1));
		}
		assertThat(store.stored_token_count()).isEqualTo(3);
		assertThat(store.is_valid(tokens.get(0))).isFalse();
		assertThat(store.is_valid(tokens.get(1))).isFalse();
		assertThat(store.is_valid(tokens.get(2))).isTrue();
		assertThat(store.is_valid(tokens.get(3))).isTrue();
		assertThat(store.is_valid(tokens.get(4))).isTrue();
	}

	@Test
	void concurrent_logins_keep_every_token_within_limit_valid() throws Exception {
		AdminTokenStore store = store(Duration.ofHours(1), 500);
		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Callable<String>> tasks = new ArrayList<>();
			for (int i = 0; i < 200; i++) {
				tasks.add(store::issue_token);
			}
			Set<String> tokens = new HashSet<>();
			for (Future<String> future : executor.invokeAll(tasks)) {
				tokens.add(future.get());
			}
			assertThat(tokens).hasSize(200);
			assertThat(tokens).allMatch(store::is_valid);
			assertThat(store.stored_token_count()).isEqualTo(200);
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void concurrent_logins_never_exceed_max_tokens() throws Exception {
		AdminTokenStore store = store(Duration.ofHours(1), 50);
		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Callable<String>> tasks = new ArrayList<>();
			for (int i = 0; i < 400; i++) {
				tasks.add(store::issue_token);
			}
			executor.invokeAll(tasks);
			assertThat(store.stored_token_count()).isEqualTo(50);
		}
		finally {
			executor.shutdownNow();
		}
	}

}
