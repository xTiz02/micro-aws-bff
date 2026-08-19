package com.prd.bffclient.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Espejo del ProductResponse de catalog-service. Contrato propio y desacoplado a propósito:
 * el BFF no comparte DTOs de dominio con los servicios internos, solo lo genérico
 * (ApiError, PageResponse) vive en starter-common. */
public record CatalogProductDto(
    UUID id,
    String name,
    String description,
    String category,
    BigDecimal price,
    Integer stock,
    Boolean active
) {
}
