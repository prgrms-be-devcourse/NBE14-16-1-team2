package com.cafe.beanorder.global.error;

/**
 * 모든 오류 응답의 본문. 팀 정책에 따라 code 와 message 두 필드만 사용한다.
 *
 * <pre>
 * { "code": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다." }
 * </pre>
 */
public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}

}
