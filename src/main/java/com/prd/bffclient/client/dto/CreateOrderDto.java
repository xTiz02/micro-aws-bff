package com.prd.bffclient.client.dto;

import java.util.List;

public record CreateOrderDto(
    String customerId,
    List<CreateOrderItemDto> items
) {
}
