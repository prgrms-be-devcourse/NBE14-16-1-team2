package com.cafe.beanorder.order;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // GET /api/admin/orders?status=ORDERED&deliveryDate=2026-11-27&email=a@test.com
    @GetMapping
    public List<OrderResponse> get_orders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(required = false) String email) {
        return adminOrderService.get_orders(status, deliveryDate, email);
    }

    // GET /api/admin/orders/12
    @GetMapping("/{orderId}")
    public OrderResponse get_order(@PathVariable Long orderId) {
        return adminOrderService.get_order(orderId);
    }
}
