package com.cafe.beanorder.order;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DeliveryDateCalculator {

    private static final LocalTime CRITERIA = LocalTime.of(14,00) ;

    public LocalDate calculate_date(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        if(time.isBefore(CRITERIA)) return date;
        else return date.plusDays(1);
    }

}
