/**
 * 상품(원두) 도메인. 엔티티 Product·Origin 과 상품 Repository·Service·API 가 있다.
 *
 * <ul>
 *   <li>고객 API(ProductController, /api/products): 삭제되지 않은 상품만 조회한다.</li>
 *   <li>관리자 API(AdminProductController, /api/admin/products): 삭제 상품을 포함해 조회하고, 등록·수정·논리 삭제를 한다.
 *       관리자 토큰 검사는 global.security 가 담당한다.</li>
 *   <li>ProductDataInitializer: local 프로필에서 상품 테이블이 비어 있으면 기본 상품을 넣는다.</li>
 * </ul>
 */
package com.cafe.beanorder.product;
