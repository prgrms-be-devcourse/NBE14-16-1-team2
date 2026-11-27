// 주문 전체 요청

package com.cafe.beanorder.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record OrderCreateRequest (
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
        String email,

        @NotBlank(message = "주소를 입력해주세요.")
        @Size(max = 255, message = "주소는 255자 이하로 입력해 주세요.")
        String address,

        @NotBlank(message = "우편번호를 입력해주세요.")
        @Pattern(regexp = "\\d{5}", message = "우편번호는 숫자 5자리로 입력해 주세요.") // 한국 우편번호는 숫자 5자리로 고정
        String zipcode,

        // null 및 빈 배열 방지
        @NotEmpty(message = "주문할 상품을 하나 이상 담아 주세요.")
        List<@Valid OrderItemRequest> items
)
{
}
