package com.cafe.beanorder.order;

import com.cafe.beanorder.product.Origin;
import com.cafe.beanorder.product.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // ===== 조회 (5.1) =====

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

    // ===== 취소 (5.2) =====

    @Test
    void cancel_order_changes_status_to_cancelled() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 이미 취소된 주문을 다시 취소하면 409
        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_CONFLICT"));
    }

    @Test
    void change_status_rejects_other_than_cancelled() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // ===== 삭제 (5.2) =====

    @Test
    void delete_order_removes_order_and_items() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));

        mockMvc.perform(delete("/api/admin/orders/{orderId}", order.getId()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        Number itemCount = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM order_item WHERE order_id = ?1")
                .setParameter(1, order.getId())
                .getSingleResult();
        assertThat(itemCount.longValue()).isZero();

        mockMvc.perform(get("/api/admin/orders/{orderId}", order.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_order_returns_404_when_missing() throws Exception {
        mockMvc.perform(delete("/api/admin/orders/{orderId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    // ===== 수정 (5.2) =====

    @Test
    void update_order_replaces_address_and_items() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));
        Long ethiopiaId = save_product("에티오피아", 6000, false);

        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시 마포구 월드컵로 2", "03900", ethiopiaId, 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.address").value("서울시 마포구 월드컵로 2"))
                .andExpect(jsonPath("$.zipcode").value("03900"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("에티오피아"))
                .andExpect(jsonPath("$.totalPrice").value(24000));

        entityManager.flush();
        entityManager.clear();

        // 예전 주문 상품 2줄은 지워지고 새 1줄만 남아야 함
        Number itemCount = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM order_item WHERE order_id = ?1")
                .setParameter(1, order.getId())
                .getSingleResult();
        assertThat(itemCount.longValue()).isEqualTo(1);
    }

    @Test
    void update_order_keeps_original_price_for_existing_items() throws Exception {
        Long colombiaId = save_product("콜롬비아", 5000, false);
        Long ethiopiaId = save_product("에티오피아", 6000, false);

        Product colombia = entityManager.find(Product.class, colombiaId);
        Order order = Order.create("a@test.com", "서울시", "06234",
                LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));
        order.add_order_item(colombia, 2);
        entityManager.persist(order);

        // 주문 후 관리자가 콜롬비아 가격을 9000원으로 올림
        colombia.update_info("콜롬비아", 9000, null, Origin.COLOMBIA, null);
        entityManager.flush();
        entityManager.clear();

        String body = """
                {"address":"서울시","zipcode":"06234","items":[
                  {"productId":%d,"quantity":5},
                  {"productId":%d,"quantity":1}
                ]}
                """.formatted(colombiaId, ethiopiaId);

        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderPrice").value(5000))   // 기존 상품: 주문 당시 가격 유지
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[1].orderPrice").value(6000))   // 새 상품: 현재 가격
                .andExpect(jsonPath("$.totalPrice").value(31000));         // 5000×5 + 6000×1
    }

    @Test
    void update_order_merges_same_product_lines() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));
        Long ethiopiaId = save_product("에티오피아", 6000, false);

        String body = """
                {"address":"서울시","zipcode":"06234","items":[
                  {"productId":%d,"quantity":2},
                  {"productId":%d,"quantity":3}
                ]}
                """.formatted(ethiopiaId, ethiopiaId);

        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))     // 두 줄이 한 줄로 합쳐짐
                .andExpect(jsonPath("$.items[0].quantity").value(5))  // 2 + 3
                .andExpect(jsonPath("$.totalPrice").value(30000));    // 6000 × 5
    }

    @Test
    void update_order_merges_into_order_with_same_email_date_address() throws Exception {
        Long colombiaId = save_product("콜롬비아", 5000, false);
        Long ethiopiaId = save_product("에티오피아", 6000, false);
        Product colombia = entityManager.find(Product.class, colombiaId);
        LocalDate deliveryDate = LocalDate.of(2026, 11, 26);

        // 원래 있던 주문 B (마포)
        Order target = Order.create("a@test.com", "서울시 마포구 월드컵로 2", "03900",
                LocalDateTime.of(2026, 11, 26, 9, 0), deliveryDate);
        target.add_order_item(colombia, 1);
        entityManager.persist(target);

        // 수정할 주문 A (강남)
        Order edited = Order.create("a@test.com", "서울시 강남구 테헤란로 1", "06234",
                LocalDateTime.of(2026, 11, 26, 10, 0), deliveryDate);
        edited.add_order_item(colombia, 2);
        entityManager.persist(edited);
        entityManager.flush();
        entityManager.clear();

        // A의 주소를 B와 같게 수정 → B로 병합
        String body = """
                {"address":"서울시 마포구 월드컵로 2","zipcode":"03900","items":[
                  {"productId":%d,"quantity":2},
                  {"productId":%d,"quantity":1}
                ]}
                """.formatted(colombiaId, ethiopiaId);

        mockMvc.perform(put("/api/admin/orders/{orderId}", edited.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(target.getId()))      // 남은 주문은 B
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].quantity").value(3))         // 콜롬비아 1 + 2
                .andExpect(jsonPath("$.items[1].productName").value("에티오피아"))
                .andExpect(jsonPath("$.totalPrice").value(21000));          // 5000×3 + 6000×1

        // A는 사라짐
        mockMvc.perform(get("/api/admin/orders/{orderId}", edited.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_order_merge_ignores_leading_and_trailing_spaces_only() throws Exception {
        Long colombiaId = save_product("콜롬비아", 5000, false);
        Product colombia = entityManager.find(Product.class, colombiaId);
        LocalDate deliveryDate = LocalDate.of(2026, 11, 26);

        // 원래 있던 주문 B (마포)
        Order target = Order.create("a@test.com", "서울시 마포구 월드컵로 2", "03900",
                LocalDateTime.of(2026, 11, 26, 9, 0), deliveryDate);
        target.add_order_item(colombia, 1);
        entityManager.persist(target);

        // 수정할 주문 C (강남) - 주소 중간 띄어쓰기만 다르게 수정할 예정
        Order spaceDifferent = Order.create("a@test.com", "서울시 강남구 테헤란로 1", "06234",
                LocalDateTime.of(2026, 11, 26, 10, 0), deliveryDate);
        spaceDifferent.add_order_item(colombia, 1);
        entityManager.persist(spaceDifferent);

        // 수정할 주문 A (강남) - 앞뒤 공백만 붙여서 수정할 예정
        Order edited = Order.create("a@test.com", "서울시 강남구 테헤란로 1", "06234",
                LocalDateTime.of(2026, 11, 26, 11, 0), deliveryDate);
        edited.add_order_item(colombia, 2);
        entityManager.persist(edited);
        entityManager.flush();
        entityManager.clear();

        // 중간 띄어쓰기가 다르면 다른 주소 → 병합 안 됨
        mockMvc.perform(put("/api/admin/orders/{orderId}", spaceDifferent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시 마포구 월드컵로2", "03900", colombiaId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(spaceDifferent.getId()));

        // 앞뒤 공백만 다르면 같은 주소 → B로 병합
        mockMvc.perform(put("/api/admin/orders/{orderId}", edited.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("  서울시 마포구 월드컵로 2  ", "03900", colombiaId, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(target.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(3));   // 1 + 2
    }

    @Test
    void update_order_returns_409_when_not_ordered() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));
        Long ethiopiaId = save_product("에티오피아", 6000, false);

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시", "03900", ethiopiaId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_CONFLICT"));
    }

    @Test
    void update_order_returns_404_when_product_or_order_missing() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));
        Long deletedId = save_product("단종 원두", 7000, true);
        Long ethiopiaId = save_product("에티오피아", 6000, false);

        // 삭제된 상품
        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시", "03900", deletedId, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        // 없는 상품
        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시", "03900", 999999L, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        // 없는 주문
        mockMvc.perform(put("/api/admin/orders/{orderId}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시", "03900", ethiopiaId, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void update_order_rejects_invalid_body() throws Exception {
        Order order = save_order("a@test.com", LocalDateTime.of(2026, 11, 26, 13, 0), LocalDate.of(2026, 11, 26));
        Long ethiopiaId = save_product("에티오피아", 6000, false);

        // 상품 목록이 비어 있음
        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"서울시\",\"zipcode\":\"03900\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        // 수량 0
        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body("서울시", "03900", ethiopiaId, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        // 주소 빈칸
        mockMvc.perform(put("/api/admin/orders/{orderId}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update_body(" ", "03900", ethiopiaId, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // ===== 도우미 =====

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

    private Long save_product(String name, int price, boolean deleted) {
        Product product = Product.create(name, price, null, Origin.COLOMBIA, null);
        if (deleted) {
            product.mark_deleted();
        }
        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();
        return product.getId();
    }

    private String update_body(String address, String zipcode, Long productId, int quantity) {
        return """
                {"address":"%s","zipcode":"%s","items":[{"productId":%d,"quantity":%d}]}
                """.formatted(address, zipcode, productId, quantity);
    }

}