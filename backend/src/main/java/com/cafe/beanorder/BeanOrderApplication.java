package com.cafe.beanorder;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// 관리자 인증은 ADMIN_PASSWORD + 토큰으로 직접 처리한다. Spring Boot 기본 사용자(user/생성 비밀번호 로그 출력)는 만들지 않는다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BeanOrderApplication {

	public static final String SERVICE_TIME_ZONE = "Asia/Seoul";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(SERVICE_TIME_ZONE));
		SpringApplication.run(BeanOrderApplication.class, args);
	}

}
