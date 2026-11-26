package com.cafe.beanorder.admin;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 로그인 API. 보안 설정에서 정확히 POST /api/admin/login 만 인증 없이 허용한다.
 */
@RestController
@RequiredArgsConstructor
public class AdminLoginController {

	private final AdminAuthService adminAuthService;

	@PostMapping("/api/admin/login")
	public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
		return adminAuthService.login(request.password());
	}

}
