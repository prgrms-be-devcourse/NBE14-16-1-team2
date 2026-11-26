package com.cafe.beanorder.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** 테스트에서 현재 시각을 직접 정하고 앞으로 이동시킬 수 있는 Clock. */
public class MutableTestClock extends Clock {

	private final ZoneId zone;
	private Instant instant;

	public MutableTestClock(Instant instant, ZoneId zone) {
		this.instant = instant;
		this.zone = zone;
	}

	public void set_instant(Instant instant) {
		this.instant = instant;
	}

	public void advance(Duration duration) {
		instant = instant.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableTestClock(instant, zone);
	}

	@Override
	public Instant instant() {
		return instant;
	}

}
