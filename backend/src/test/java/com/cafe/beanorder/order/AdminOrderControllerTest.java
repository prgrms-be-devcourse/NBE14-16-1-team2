package com.cafe.beanorder.order;

import com.cafe.beanorder.product.Origin;
import com.cafe.beanorder.product.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrderControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void set_up() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void get_order_returns_order_with_items() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));

        mockMvc.perform(get("/api/admin/orders/{orderId}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.totalPrice").value(15000))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void get_order_returns_404_when_missing() throws Exception {
        mockMvc.perform(get("/api/admin/orders/{orderId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void get_orders_filters_and_sorts_latest_first() throws Exception {
        save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 10, 0), LocalDate.of(2026, 11, 26));
        save_order("b@test.com", LocalDateTime.of(2026, 11, 26, 15, 0), LocalDate.of(2026, 11, 27));
        save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 16, 0), LocalDate.of(2026, 11, 27));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].orderedAt").value("2026-11-26T16:00:00"));

        mockMvc.perform(get("/api/admin/orders").param("email", "a@test.com").param("deliveryDate", "2026-11-27"))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/admin/orders").param("status", "ABC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private Order save_order(String email, LocalDateTime orderedAt, LocalDate deliveryDate) {
        Product colombia = Product.create("콜롬비아", 5000, null, Origin.COLOMBIA, null);
        entityManager.persist(colombia);
        Order order = Order.create(email, "서울시 강남구 테헤란로 1", "06234", orderedAt, deliveryDate);
        order.add_order_item(colombia, 2);
        order.add_order_item(colombia, 1);
        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();
        return order;
    }

}