package com.prd.bffclient.client;

import com.prd.bffclient.client.dto.CreateOrderDto;
import com.prd.bffclient.client.dto.OrderDto;
import com.prd.bffclient.config.ClientsProperties;
import com.prd.common.constant.SecurityConstants;
import com.prd.common.dto.PageResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OrderClient {

  private final WebClient orderWebClient;
  private final CircuitBreaker orderCircuitBreaker;
  private final Duration timeout;

  public OrderClient(
      WebClient orderWebClient,
      CircuitBreaker orderCircuitBreaker,
      ClientsProperties properties) {
    this.orderWebClient = orderWebClient;
    this.orderCircuitBreaker = orderCircuitBreaker;
    this.timeout = Duration.ofMillis(properties.orderService().timeoutMs());
  }

  public Mono<OrderDto> createOrder(String idempotencyKey, CreateOrderDto request) {
    return orderWebClient.post()
        .uri("/orders")
        .header(SecurityConstants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(OrderDto.class)
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(orderCircuitBreaker));
  }

  public Mono<OrderDto> getOrder(UUID id) {
    return orderWebClient.get()
        .uri("/orders/{id}", id)
        .retrieve()
        .bodyToMono(OrderDto.class)
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(orderCircuitBreaker));
  }

  public Mono<PageResponse<OrderDto>> getOrders(String customerId, int page, int size) {
    return orderWebClient.get()
        .uri(uriBuilder -> uriBuilder.path("/orders")
            .queryParam("customerId", customerId)
            .queryParam("page", page)
            .queryParam("size", size)
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<PageResponse<OrderDto>>() {
        })
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(orderCircuitBreaker));
  }
}
