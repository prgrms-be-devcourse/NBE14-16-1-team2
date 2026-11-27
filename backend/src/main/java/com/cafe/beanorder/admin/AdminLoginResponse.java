package com.cafe.beanorder.admin;

/** 관리자 로그인 성공 응답. 래퍼 없이 token 만 반환한다. (비밀번호는 포함하지 않음) */
public record AdminLoginResponse(String token) {

	@Override
	public String toString() {
		return "AdminLoginResponse[token=[PROTECTED]]";
	}

}
