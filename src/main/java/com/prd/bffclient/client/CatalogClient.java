package com.prd.bffclient.client;

import com.prd.bffclient.client.dto.CatalogProductDto;
import com.prd.bffclient.config.ClientsProperties;
import com.prd.common.dto.PageResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CatalogClient {

  private final WebClient catalogWebClient;
  private final CircuitBreaker catalogCircuitBreaker;
  private final Duration timeout;

  public CatalogClient(
      WebClient catalogWebClient,
      CircuitBreaker catalogCircuitBreaker,
      ClientsProperties properties) {
    this.catalogWebClient = catalogWebClient;
    this.catalogCircuitBreaker = catalogCircuitBreaker;
    this.timeout = Duration.ofMillis(properties.catalogService().timeoutMs());
  }

  public Mono<CatalogProductDto> getProduct(UUID id) {
    return catalogWebClient.get()
        .uri("/products/{id}", id)
        .retrieve()
        .bodyToMono(CatalogProductDto.class)
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(catalogCircuitBreaker));
  }

  public Flux<CatalogProductDto> getProductsByIds(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Flux.empty();
    }
    return catalogWebClient.post()
        .uri("/products/batch")
        .bodyValue(ids)
        .retrieve()
        .bodyToFlux(CatalogProductDto.class)
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(catalogCircuitBreaker));
  }

  public Mono<PageResponse<CatalogProductDto>> getProducts(int page, int size) {
    return catalogWebClient.get()
        .uri(uriBuilder -> uriBuilder.path("/products")
            .queryParam("page", page)
            .queryParam("size", size)
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<PageResponse<CatalogProductDto>>() {
        })
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(catalogCircuitBreaker));
  }
}
