package com.cafe.beanorder.order;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;
import com.cafe.beanorder.product.Product;
import com.cafe.beanorder.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

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

    // 수정: 주소·우편번호 변경 + 상품 목록 교체 (ORDERED 일 때만)
    // 수정 후 같은 이메일·배송일·주소의 다른 주문이 있으면 그 주문으로 병합하고, 병합된 주문을 반환한다.
    @Transactional
    public OrderResponse update_order(Long orderId, OrderUpdateRequest request) {
        Order order = find_order(orderId);

        // 같은 상품이 여러 줄로 오면 수량을 합쳐 1줄로 만든다 (보낸 순서 유지)
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            quantityByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // 상품을 먼저 모두 찾아 둔다. 없거나 삭제된 상품이면 404
        List<Product> products = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        quantityByProductId.forEach((productId, quantity) -> {
            products.add(find_product(productId));
            quantities.add(quantity);
        });

        order.update(request.address(), request.zipcode());
        order.replace_order_items(products, quantities);

        // 병합: 같은 이메일·배송일·주소의 다른 주문이 있으면 그 주문으로 합치고, 수정한 주문은 삭제
        List<Order> mergeTargets = orderRepository.find_merge_targets(order.getId(), order.getEmail(),
                order.getDeliveryDate(), order.getAddress(), OrderStatus.ORDERED);
        if (!mergeTargets.isEmpty()) {
            Order target = mergeTargets.get(0);
            target.merge_from(order);
            orderRepository.delete(order);
            orderRepository.flush();
            return OrderResponse.from(target);
        }

        // 새 주문 상품에 번호(id)가 붙도록 DB에 바로 반영
        orderRepository.flush();
        return OrderResponse.from(order);
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

    // 삭제: 주문과 주문 상세를 함께 삭제
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

    // 공통: 없거나 삭제된 상품이면 404 (고객에게 안 보이는 상품은 주문에도 못 넣음)
    private Product find_product(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

}
