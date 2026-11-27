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
        return OrderResponse.from(find_order(orderId));
    }

    // 상태 변경: 지금은 CANCELLED 만 허용
    @Transactional
    public OrderStatusResponse change_status(Long orderId, OrderStatusRequest request) {
        if (request.status() != OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Order order = find_order(orderId);
        order.cancel();
        return OrderStatusResponse.from(order);
    }

    // 삭제: 주문을 지우면 DB 규칙(ON DELETE CASCADE)으로 주문 상세도 함께 지워짐
    @Transactional
    public void delete_order(Long orderId) {
        Order order = find_order(orderId);
        orderRepository.delete(order);
    }

    // 공통: 번호로 찾고 없으면 404
    private Order find_order(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

}
