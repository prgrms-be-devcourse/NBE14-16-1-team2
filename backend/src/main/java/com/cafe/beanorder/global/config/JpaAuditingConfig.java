package com.cafe.beanorder.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.cafe.beanorder.BeanOrderApplication;

/**
 * JPA Auditing 설정. @CreatedDate·@LastModifiedDate 값을 Asia/Seoul 기준 Clock 으로 기록한다.
 * 테스트에서는 Clock 빈을 바꿔 시간을 제어할 수 있다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of(BeanOrderApplication.SERVICE_TIME_ZONE));
	}

	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}

}
