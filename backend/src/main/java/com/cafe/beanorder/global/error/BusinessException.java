package com.cafe.beanorder.global.error;

import lombok.Getter;

/**
 * 업무 규칙 위반을 표현하는 예외. GlobalExceptionHandler 가 ErrorCode 의 상태와 code/message 로 변환한다.
 *
 * <pre>
 * throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}
