package com.cafe.beanorder.admin;

import jakarta.validation.constraints.NotBlank;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * 관리자 로그인 요청. password 는 JSON 문자열만 허용한다. (숫자·boolean 을 문자열로 자동 변환하지 않음)
 * 비밀번호가 로그에 남지 않도록 toString 을 재정의한다.
 */
public record AdminLoginRequest(
		@NotBlank(message = "비밀번호를 입력해 주세요.")
		@JsonDeserialize(using = StrictStringDeserializer.class)
		String password) {

	@Override
	public String toString() {
		return "AdminLoginRequest[password=[PROTECTED]]";
	}

}
