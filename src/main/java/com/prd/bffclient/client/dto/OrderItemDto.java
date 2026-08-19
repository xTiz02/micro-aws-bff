package com.prd.bffclient.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
    UUID productId,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
