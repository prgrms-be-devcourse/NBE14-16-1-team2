package com.cafe.beanorder.order;

/**
 * 주문 상태. DB 에는 이름 문자열로 저장한다.
 * 상태 전환 규칙(스케줄러·관리자 취소 등)은 아직 구현하지 않았다.
 */
public enum OrderStatus {

	ORDERED,
	SHIPPING,
	DELIVERED,
	CANCELLED

}
