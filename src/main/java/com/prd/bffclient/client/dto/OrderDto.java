package com.prd.bffclient.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
    UUID id,
    String customerId,
    String status,
    BigDecimal totalAmount,
    List<OrderItemDto> items,
    Instant createdAt
) {
}
