package com.prd.bffclient.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CheckoutItemRequest(
    @NotNull UUID productId,
    @NotNull @Positive Integer quantity
) {
}
