package com.cafe.beanorder;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeanOrderApplication {

	public static final String SERVICE_TIME_ZONE = "Asia/Seoul";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(SERVICE_TIME_ZONE));
		SpringApplication.run(BeanOrderApplication.class, args);
	}

}
