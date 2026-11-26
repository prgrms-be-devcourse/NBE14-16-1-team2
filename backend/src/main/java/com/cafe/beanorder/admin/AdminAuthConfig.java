package com.cafe.beanorder.admin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 관리자 인증 설정(app.admin.*)을 등록한다. 설정이 잘못되면 이 단계에서 서버 시작이 중단된다. */
@Configuration
@EnableConfigurationProperties(AdminAuthProperties.class)
public class AdminAuthConfig {
}
