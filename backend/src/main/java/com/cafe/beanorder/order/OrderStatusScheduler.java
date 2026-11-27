package com.cafe.beanorder.order;

import com.cafe.beanorder.BeanOrderApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 배송 상태 자동 전환 스케줄러 (README 2-3).
 * 매일 14:30 (Asia/Seoul)
 * 1) SHIPPING → DELIVERED : 배송 예정일이 어제 이하 (배송 시작 다음 날)
 * 2) ORDERED  → SHIPPING  : 배송 예정일이 오늘 이하 (놓친 날 포함)
 * 순서가 중요하다. 1을 먼저 해야 오늘 배송 시작한 주문이 같은 실행에서 바로 배송 완료되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;
    private final Clock clock;

    @Scheduled(cron = "0 30 14 * * *", zone = BeanOrderApplication.SERVICE_TIME_ZONE)
    @Transactional
    public void update_order_statuses() {
        LocalDate today = LocalDate.now(clock);

        int deliveredCount = orderRepository.update_status_until(
                OrderStatus.SHIPPING, OrderStatus.DELIVERED, today.minusDays(1));
        int shippingCount = orderRepository.update_status_until(
                OrderStatus.ORDERED, OrderStatus.SHIPPING, today);

        log.info("배송 상태 자동 전환 ({}): 배송완료 {}건, 배송시작 {}건", today, deliveredCount, shippingCount);
    }

}