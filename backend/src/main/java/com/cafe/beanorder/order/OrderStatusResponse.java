package com.cafe.beanorder.order;

/** 관리자 주문 상태 변경 응답. */
public record OrderStatusResponse(Long orderId, OrderStatus status) {

	public static OrderStatusResponse from(Order order) {
		return new OrderStatusResponse(order.getId(), order.getStatus());
	}

}
