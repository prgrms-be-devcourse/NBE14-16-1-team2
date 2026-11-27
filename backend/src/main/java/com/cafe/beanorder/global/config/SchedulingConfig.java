package com.cafe.beanorder.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러(@Scheduled) 사용 설정.
 * BeanOrderApplication 을 건드리지 않도록 별도 설정 파일에 둔다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
