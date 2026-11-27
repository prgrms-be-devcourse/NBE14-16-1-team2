package com.cafe.beanorder.order;

import com.cafe.beanorder.support.MutableTestClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderStatusSchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 30);

    /**
     * 테스트 시계: 2026-11-30 14:30 (한국 시간)으로 고정
     */
    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        MutableTestClock mutableTestClock() {
            return new MutableTestClock(TODAY.atTime(14, 30).atZone(SEOUL).toInstant(), SEOUL);
        }

    }

    @Autowired
    private OrderStatusScheduler orderStatusScheduler;

    @Autowired
    private MutableTestClock clock;

    @Autowired
    private EntityManager entityManager;

    @Test
    void ships_today_and_overdue_orders_only() {
        clock.set_instant(TODAY.atTime(14, 30).atZone(SEOUL).toInstant());

        Long todayOrder = save_order(TODAY);                  // 오늘 배송 → 배송중
        Long overdueOrder = save_order(TODAY.minusDays(1));   // 어제 놓친 주문 → 배송중 (선택지 A)
        Long tomorrowOrder = save_order(TODAY.plusDays(1));   // 내일 배송 → 그대로
        Long cancelledOrder = save_cancelled_order(TODAY);    // 취소 → 그대로

        orderStatusScheduler.update_order_statuses();

        assertThat(status_of(todayOrder)).isEqualTo(OrderStatus.SHIPPING);
        assertThat(status_of(overdueOrder)).isEqualTo(OrderStatus.SHIPPING);
        assertThat(status_of(tomorrowOrder)).isEqualTo(OrderStatus.ORDERED);
        assertThat(status_of(cancelledOrder)).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void delivers_on_next_day_not_same_day() {
        clock.set_instant(TODAY.atTime(14, 30).atZone(SEOUL).toInstant());
        Long orderId = save_order(TODAY);

        // 오늘 14:30: 배송 시작
        orderStatusScheduler.update_order_statuses();
        assertThat(status_of(orderId)).isEqualTo(OrderStatus.SHIPPING);

        // 같은 날 한 번 더 돌아도 배송 완료되지 않음
        orderStatusScheduler.update_order_statuses();
        assertThat(status_of(orderId)).isEqualTo(OrderStatus.SHIPPING);

        // 다음 날 14:30: 배송 완료
        clock.advance(Duration.ofDays(1));
        orderStatusScheduler.update_order_statuses();
        assertThat(status_of(orderId)).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void catches_up_after_missed_day() {
        // 11-30 배송 주문인데, 서버가 꺼져서 11-30 실행을 놓치고 12-01에 처음 실행됨
        clock.set_instant(TODAY.plusDays(1).atTime(14, 30).atZone(SEOUL).toInstant());
        Long orderId = save_order(TODAY);

        // 12-01: 밀린 주문이 배송 시작 (같은 실행에서 바로 배송 완료되지는 않음)
        orderStatusScheduler.update_order_statuses();
        assertThat(status_of(orderId)).isEqualTo(OrderStatus.SHIPPING);

        // 12-02: 배송 완료
        clock.advance(Duration.ofDays(1));
        orderStatusScheduler.update_order_statuses();
        assertThat(status_of(orderId)).isEqualTo(OrderStatus.DELIVERED);
    }

    // ===== 도우미 =====

    private Long save_order(LocalDate deliveryDate) {
        Order order = Order.create("a@test.com", "서울시", "06234",
                LocalDateTime.of(2026, 11, 26, 13, 0), deliveryDate);
        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();
        return order.getId();
    }

    private Long save_cancelled_order(LocalDate deliveryDate) {
        Order order = Order.create("b@test.com", "서울시", "06234",
                LocalDateTime.of(2026, 11, 26, 13, 0), deliveryDate);
        order.cancel();
        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();
        return order.getId();
    }

    private OrderStatus status_of(Long orderId) {
        entityManager.clear();
        return entityManager.find(Order.class, orderId).getStatus();
    }

}