package com.cafe.beanorder.global.error;

/**
 * 엔티티 생성·변경 시 필수 값과 양수 조건을 확인한다.
 * 위반 시 BusinessException(INVALID_INPUT) 을 던져 API 에서는 400 으로 응답된다.
 */
public final class DomainPreconditions {

	private DomainPreconditions() {
	}

	public static String require_text(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw invalid(fieldName + ": 값이 필요합니다.");
		}
		return value;
	}

	public static <T> T require_value(T value, String fieldName) {
		if (value == null) {
			throw invalid(fieldName + ": 값이 필요합니다.");
		}
		return value;
	}

	public static int require_positive(int value, String fieldName) {
		if (value <= 0) {
			throw invalid(fieldName + ": 0보다 커야 합니다.");
		}
		return value;
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.INVALID_INPUT, message);
	}

}
