package com.cafe.beanorder.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    // PUT /api/admin/orders/12  body: {"address":..., "zipcode":..., "items":[{"productId":1,"quantity":3}]}
    @PutMapping("/{orderId}")
    public OrderResponse update_order(@PathVariable Long orderId,
                                      @Valid @RequestBody OrderUpdateRequest request) {
        return adminOrderService.update_order(orderId, request);
    }

    // PUT /api/admin/orders/12/status  body: {"status":"CANCELLED"}
    @PutMapping("/{orderId}/status")
    public OrderStatusResponse change_status(@PathVariable Long orderId,
                                             @Valid @RequestBody OrderStatusRequest request) {
        return adminOrderService.change_status(orderId, request);
    }

    // DELETE /api/admin/orders/12 → 204
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete_order(@PathVariable Long orderId) {
        adminOrderService.delete_order(orderId);
        return ResponseEntity.noContent().build();
    }

}
