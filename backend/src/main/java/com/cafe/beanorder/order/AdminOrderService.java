package com.cafe.beanorder.order;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

	private final OrderRepository orderRepository;

	// 목록: 조건으로 찾아서 → 하나씩 포장
	public List<OrderResponse> get_orders(OrderStatus status, LocalDate deliveryDate, String email) {
		return orderRepository.search_orders(status, deliveryDate, email).stream()
			.map(OrderResponse::from)
			.toList();
	}

	// 상세: 번호로 찾아서 → 없으면 404 → 있으면 포장
	public OrderResponse get_order(Long orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
		return OrderResponse.from(order);
	}

}
