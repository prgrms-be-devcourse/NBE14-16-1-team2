package com.cafe.beanorder.global.dbcheck;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB 연결 검증 전용 엔티티.
 * 테스트 소스(src/test)에만 존재하므로 실제 앱 도메인과 local DB 테이블에 포함되지 않는다.
 */
@Entity
@Table(name = "db_check")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DbCheckEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String label;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public DbCheckEntity(String label, LocalDateTime createdAt) {
		this.label = label;
		this.createdAt = createdAt;
	}

}
