package com.cafe.beanorder.order;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        int quantity,
        int orderPrice
) {
    // 엔티티(OrderItem)를 응답 모양으로 바꾸는 메서드
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getOrderPrice()
        );
    }
}
