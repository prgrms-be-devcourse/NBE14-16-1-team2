package com.cafe.beanorder.order;

import jakarta.validation.constraints.NotNull;

/** 관리자 주문 상태 변경 요청. 현재는 CANCELLED 만 허용한다. */
public record OrderStatusRequest(
		@NotNull OrderStatus status) {
}