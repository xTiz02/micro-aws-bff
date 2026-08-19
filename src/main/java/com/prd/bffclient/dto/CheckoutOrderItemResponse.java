package com.prd.bffclient.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutOrderItemResponse(
    UUID productId,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
