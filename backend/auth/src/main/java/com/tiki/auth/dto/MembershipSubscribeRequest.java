package com.tiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipSubscribeRequest {

    @NotBlank(message = "Mã gói thành viên không được để trống")
    private String planCode;

    @Builder.Default
    private String billingCycle = "MONTHLY"; // MONTHLY or YEARLY

    private String paymentMethodId;
    private Boolean startTrial;
}
