package io.github.datnguyen2810.shopcore.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShippingAddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    private String recipientName;

    @NotBlank(message = "Recipient phone is required")
    @Size(max = 32, message = "Recipient phone must not exceed 32 characters")
    private String recipientPhone;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must not exceed 255 characters")
    private String addressLine;

    @NotBlank(message = "Ward is required")
    @Size(max = 120, message = "Ward must not exceed 120 characters")
    private String ward;

    @NotBlank(message = "District is required")
    @Size(max = 120, message = "District must not exceed 120 characters")
    private String district;

    @NotBlank(message = "City is required")
    @Size(max = 120, message = "City must not exceed 120 characters")
    private String city;
}
