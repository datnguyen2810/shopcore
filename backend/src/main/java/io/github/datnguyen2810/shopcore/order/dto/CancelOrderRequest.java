package io.github.datnguyen2810.shopcore.order.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelOrderRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
