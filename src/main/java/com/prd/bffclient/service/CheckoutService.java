package com.prd.bffclient.service;

import com.prd.bffclient.client.CatalogClient;
import com.prd.bffclient.client.OrderClient;
import com.prd.bffclient.client.dto.CatalogProductDto;
import com.prd.bffclient.client.dto.CreateOrderDto;
import com.prd.bffclient.client.dto.CreateOrderItemDto;
import com.prd.bffclient.client.dto.OrderDto;
import com.prd.bffclient.client.dto.OrderItemDto;
import com.prd.bffclient.dto.CheckoutItemRequest;
import com.prd.bffclient.dto.CheckoutOrderItemResponse;
import com.prd.bffclient.dto.CheckoutOrderResponse;
import com.prd.bffclient.dto.CreateCheckoutOrderRequest;
import com.prd.bffclient.dto.ProductSummary;
import com.prd.common.dto.PageResponse;
import com.prd.common.exception.BusinessException;
import com.prd.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CheckoutService {

  private final CatalogClient catalogClient;
  private final OrderClient orderClient;

  public CheckoutService(CatalogClient catalogClient, OrderClient orderClient) {
    this.catalogClient = catalogClient;
    this.orderClient = orderClient;
  }

  public Mono<PageResponse<ProductSummary>> browseProducts(int page, int size) {
    return catalogClient.getProducts(page, size)
        .map(pageResponse -> new PageResponse<>(
            pageResponse.content().stream().map(ProductSummary::from).toList(),
            pageResponse.page(), pageResponse.size(), pageResponse.totalElements()));
  }

  public Mono<ProductSummary> getProduct(UUID id) {
    return catalogClient.getProduct(id)
        .map(ProductSummary::from)
        .onErrorResume(WebClientResponseException.NotFound.class,
            ex -> Mono.error(new ResourceNotFoundException("product", id)));
  }

  /**
   * El flujo crítico: valida cada línea del carrito contra el catálogo real (nunca confía
   * en un precio que llegue del cliente), calcula el total en el servidor, y solo entonces
   * llama a order-service. Si una línea falla la validación, no se crea ningún pedido.
   */
  public Mono<CheckoutOrderResponse> checkout(String customerId, String idempotencyKey, CreateCheckoutOrderRequest request) {
    return Flux.fromIterable(request.items())
        .flatMap(this::validateAndPrice)
        .collectList()
        .flatMap(lines -> {
          CreateOrderDto orderRequest = new CreateOrderDto(customerId, lines.stream()
              .map(line -> new CreateOrderItemDto(line.productId(), line.quantity(), line.unitPrice()))
              .toList());

          return orderClient.createOrder(idempotencyKey, orderRequest)
              .map(orderDto -> toResponse(orderDto, namesById(lines)));
        });
  }

  private Mono<CheckoutOrderItemResponse> validateAndPrice(CheckoutItemRequest item) {
    return catalogClient.getProduct(item.productId())
        .onErrorResume(WebClientResponseException.NotFound.class,
            ex -> Mono.error(new ResourceNotFoundException("product", item.productId())))
        .flatMap(product -> validateStock(product, item.quantity()));
  }

  private Mono<CheckoutOrderItemResponse> validateStock(CatalogProductDto product, int quantity) {
    if (!Boolean.TRUE.equals(product.active())) {
      return Mono.error(new BusinessException("PRODUCT_INACTIVE", "El producto " + product.name() + " no está disponible"));
    }
    if (product.stock() < quantity) {
      return Mono.error(new BusinessException("INSUFFICIENT_STOCK", "Stock insuficiente para " + product.name()));
    }
    var lineTotal = product.price().multiply(java.math.BigDecimal.valueOf(quantity));
    return Mono.just(new CheckoutOrderItemResponse(product.id(), product.name(), quantity, product.price(), lineTotal));
  }

  public Mono<CheckoutOrderResponse> getOrder(UUID id) {
    return orderClient.getOrder(id)
        .onErrorResume(WebClientResponseException.NotFound.class,
            ex -> Mono.error(new ResourceNotFoundException("order", id)))
        .flatMap(this::enrich);
  }

  public Mono<PageResponse<CheckoutOrderResponse>> getOrders(String customerId, int page, int size) {
    return orderClient.getOrders(customerId, page, size)
        .flatMap(pageResponse -> Flux.fromIterable(pageResponse.content())
            .flatMap(this::enrich)
            .collectList()
            .map(items -> new PageResponse<>(items, pageResponse.page(), pageResponse.size(), pageResponse.totalElements())));
  }

  /**
   * Enriquece un pedido con el nombre del producto consultando el catálogo. Si el catálogo
   * está caído (circuit breaker abierto, timeout, error), el pedido igual se devuelve, solo
   * que sin el nombre: el catálogo es adorno aquí, no la fuente de verdad del pedido.
   */
  private Mono<CheckoutOrderResponse> enrich(OrderDto order) {
    List<UUID> productIds = order.items().stream().map(OrderItemDto::productId).distinct().toList();
    return catalogClient.getProductsByIds(productIds)
        .collectMap(CatalogProductDto::id, CatalogProductDto::name)
        .onErrorResume(ex -> Mono.just(Map.of()))
        .map(namesById -> toResponse(order, namesById));
  }

  private static Map<UUID, String> namesById(List<CheckoutOrderItemResponse> lines) {
    return lines.stream().collect(java.util.stream.Collectors.toMap(
        CheckoutOrderItemResponse::productId, CheckoutOrderItemResponse::productName));
  }

  private static CheckoutOrderResponse toResponse(OrderDto order, Map<UUID, String> namesById) {
    List<CheckoutOrderItemResponse> items = order.items().stream()
        .map(item -> new CheckoutOrderItemResponse(
            item.productId(), namesById.get(item.productId()), item.quantity(), item.unitPrice(), item.lineTotal()))
        .toList();
    return new CheckoutOrderResponse(order.id(), order.status(), order.totalAmount(), items, order.createdAt());
  }
}
