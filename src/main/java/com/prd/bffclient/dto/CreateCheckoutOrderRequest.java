package com.prd.bffclient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateCheckoutOrderRequest(
    @NotEmpty List<@Valid CheckoutItemRequest> items
) {
}
