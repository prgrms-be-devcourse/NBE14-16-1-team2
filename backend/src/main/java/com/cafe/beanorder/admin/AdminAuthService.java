package com.cafe.beanorder.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 로그인. 입력 비밀번호를 trim·대소문자 변환 없이 그대로 설정값과 비교하고, 일치하면 새 토큰을 발급한다.
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

	private final AdminAuthProperties adminAuthProperties;
	private final AdminTokenStore adminTokenStore;

	public AdminLoginResponse login(String password) {
		if (!matches_admin_password(password)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return new AdminLoginResponse(adminTokenStore.issue_token());
	}

	/** 길이와 무관하게 일정한 시간으로 비교하려고 양쪽 SHA-256 해시를 MessageDigest.isEqual 로 비교한다. */
	private boolean matches_admin_password(String password) {
		if (password == null) {
			return false;
		}
		return MessageDigest.isEqual(sha256(password), sha256(adminAuthProperties.password()));
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", exception);
		}
	}

}
