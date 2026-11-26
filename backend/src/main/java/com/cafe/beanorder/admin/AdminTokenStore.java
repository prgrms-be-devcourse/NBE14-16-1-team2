package com.cafe.beanorder.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 서버 메모리에 보관하는 관리자 불투명 토큰(opaque token) 저장소.
 *
 * <ul>
 *   <li>토큰: SecureRandom 32바이트를 URL-safe Base64(패딩 없음, 43자)로 인코딩. 비밀번호와 무관한 순수 난수다.</li>
 *   <li>저장: 토큰 원문 대신 SHA-256 해시를 키로, 만료 시각을 값으로 보관한다. 서버가 발급한 토큰만 유효하다.</li>
 *   <li>만료: 발급 시각 + tokenTtl. 만료 시각과 같거나 지난 시점부터 무효다. 시간은 주입된 Clock 으로 판단한다.</li>
 *   <li>여러 토큰 공존: 새 로그인은 기존 토큰을 덮어쓰지 않는다.</li>
 *   <li>누적 방지: 발급할 때 만료 토큰을 정리하고, 그래도 maxActiveTokens 이상이면 가장 먼저 만료될 토큰부터 제거한다.
 *       검증 시 만료된 토큰도 바로 제거한다.</li>
 * </ul>
 *
 * 서버 재시작 시 모든 토큰이 사라지며, 여러 서버 인스턴스 사이에서 공유되지 않는다.
 * 토큰 값은 로그에 남기지 않는다.
 */
@Component
public class AdminTokenStore {

	static final int TOKEN_BYTES = 32;

	private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

	private final Map<String, Instant> expiresAtByTokenHash = new ConcurrentHashMap<>();
	private final SecureRandom secureRandom = new SecureRandom();
	private final Clock clock;
	private final AdminAuthProperties adminAuthProperties;

	public AdminTokenStore(Clock clock, AdminAuthProperties adminAuthProperties) {
		this.clock = clock;
		this.adminAuthProperties = adminAuthProperties;
	}

	/** 새 토큰을 발급하고 원문을 반환한다. 원문은 저장하지 않는다. */
	public String issue_token() {
		byte[] randomBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(randomBytes);
		String token = TOKEN_ENCODER.encodeToString(randomBytes);
		Instant now = clock.instant();
		Instant expiresAt = now.plus(adminAuthProperties.tokenTtl());

		// 정리·상한 확인·추가를 한 번에 처리해 동시 로그인에서도 상한을 넘지 않게 한다. (검증은 잠금 없이 동작)
		synchronized (this) {
			remove_expired_tokens(now);
			while (expiresAtByTokenHash.size() >= adminAuthProperties.maxActiveTokens()) {
				remove_earliest_expiring_token();
			}
			expiresAtByTokenHash.put(hash(token), expiresAt);
		}
		return token;
	}

	/** 서버가 발급했고 아직 만료되지 않은 토큰이면 true. 만료된 토큰은 이 시점에 제거한다. */
	public boolean is_valid(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}
		String tokenHash = hash(token);
		Instant expiresAt = expiresAtByTokenHash.get(tokenHash);
		if (expiresAt == null) {
			return false;
		}
		if (!clock.instant().isBefore(expiresAt)) {
			expiresAtByTokenHash.remove(tokenHash, expiresAt);
			return false;
		}
		return true;
	}

	/** 현재 보관 중인 토큰 수 (만료됐지만 아직 정리되지 않은 토큰 포함). 테스트·점검용. */
	int stored_token_count() {
		return expiresAtByTokenHash.size();
	}

	private void remove_expired_tokens(Instant now) {
		expiresAtByTokenHash.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
	}

	private void remove_earliest_expiring_token() {
		expiresAtByTokenHash.entrySet().stream()
			.min(Map.Entry.comparingByValue(Comparator.naturalOrder()))
			.ifPresent(entry -> expiresAtByTokenHash.remove(entry.getKey(), entry.getValue()));
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", exception);
		}
	}

}
