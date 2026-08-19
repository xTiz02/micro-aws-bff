package com.prd.bffclient.dto;

import com.prd.bffclient.client.dto.CatalogProductDto;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummary(
    UUID id,
    String name,
    String description,
    String category,
    BigDecimal price,
    Integer stock
) {

  public static ProductSummary from(CatalogProductDto product) {
    return new ProductSummary(
        product.id(), product.name(), product.description(), product.category(), product.price(), product.stock());
  }
}
