package com.prd.bffclient.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutOrderResponse(
    UUID id,
    String status,
    BigDecimal totalAmount,
    List<CheckoutOrderItemResponse> items,
    Instant createdAt
) {
}
