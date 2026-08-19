package com.prd.bffclient.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Lo que el BFF envía a order-service: cantidades y precios ya validados contra
 * el catálogo. order-service confía en esto porque solo el BFF puede alcanzarlo
 * (sin target group público, ver README). */
public record CreateOrderItemDto(
    UUID productId,
    Integer quantity,
    BigDecimal unitPrice
) {
}
