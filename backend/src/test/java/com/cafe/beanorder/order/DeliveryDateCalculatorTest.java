package com.cafe.beanorder.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;


 // 14시 기준 배송일 계산 경계값 테스트.
class DeliveryDateCalculatorTest {

    private final DeliveryDateCalculator calculator = new DeliveryDateCalculator();

    // ── 핵심 경계: 13:59:59 vs 14:00:00 ──

    @Test
    void order_at_13_59_59_is_delivered_same_day() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 11, 27, 13, 59, 59);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2026, 11, 27));
    }

    @Test
    void order_at_exactly_14_00_00_is_delivered_next_day() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 11, 27, 14, 0, 0);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    // 13:59:59.999999999도 "14시 전"인지 확인!
    @Test
    void order_at_last_nanosecond_before_14_is_delivered_same_day() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 11, 27, 13, 59, 59, 999_999_999);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2026, 11, 27));
    }

    // ── 하루의 시작과 끝 ──

    @Test
    void order_at_midnight_is_delivered_same_day() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 11, 27, 0, 0, 0);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2026, 11, 27));
    }

    @Test
    void order_at_23_59_59_is_delivered_next_day() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 11, 27, 23, 59, 59);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    // ── 날짜가 넘어가는 경계 (plusDays가 알아서 처리하는지) ──

    @Test
    void order_after_14_on_month_end_is_delivered_first_day_of_next_month() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 11, 30, 15, 0);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void order_after_14_on_new_years_eve_is_delivered_next_year() {
        LocalDateTime orderedAt = LocalDateTime.of(2026, 12, 31, 15, 0);

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void order_after_14_on_feb_28_of_leap_year_is_delivered_feb_29() {
        LocalDateTime orderedAt = LocalDateTime.of(2028, 2, 28, 15, 0);   // 2028년은 윤년

        assertThat(calculator.calculate_date(orderedAt)).isEqualTo(LocalDate.of(2028, 2, 29));
    }

}