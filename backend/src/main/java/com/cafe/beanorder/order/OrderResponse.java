package com.cafe.beanorder.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String email,
        String address,
        String zipcode,
        LocalDateTime orderedAt,
        LocalDate deliveryDate,
        OrderStatus status,
        long totalPrice,
        List<OrderItemResponse> items
) {
    // 엔티티(Order)를 응답 모양으로 바꾸는 메서드
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getEmail(),
                order.getAddress(),
                order.getZipcode(),
                order.getOrderedAt(),
                order.getDeliveryDate(),
                order.getStatus(),
                order.calculate_total_amount(),   // 총금액은 엔티티에 이미 있는 메서드 사용
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}
