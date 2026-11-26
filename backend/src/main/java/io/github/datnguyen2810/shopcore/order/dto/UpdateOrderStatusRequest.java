package io.github.datnguyen2810.shopcore.order.dto;

import io.github.datnguyen2810.shopcore.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
