package com.prd.bffclient.web;

import com.prd.bffclient.dto.CheckoutOrderResponse;
import com.prd.bffclient.dto.CreateCheckoutOrderRequest;
import com.prd.bffclient.dto.ProductSummary;
import com.prd.bffclient.service.CheckoutService;
import com.prd.common.constant.ApiConstants;
import com.prd.common.constant.SecurityConstants;
import com.prd.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

  private final CheckoutService service;

  public CheckoutController(CheckoutService service) {
    this.service = service;
  }

  @GetMapping("/products")
  public Mono<PageResponse<ProductSummary>> browseProducts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "" + ApiConstants.DEFAULT_PAGE_SIZE) int size) {
    return service.browseProducts(page, Math.min(size, ApiConstants.MAX_PAGE_SIZE));
  }

  @GetMapping("/products/{id}")
  public Mono<ProductSummary> getProduct(@PathVariable UUID id) {
    return service.getProduct(id);
  }

  @PostMapping("/orders")
  public Mono<ResponseEntity<CheckoutOrderResponse>> checkout(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = SecurityConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
      @Valid @RequestBody CreateCheckoutOrderRequest request) {
    String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
    return service.checkout(jwt.getSubject(), key, request)
        .map(order -> ResponseEntity.status(HttpStatus.CREATED).body(order));
  }

  @GetMapping("/orders/{id}")
  public Mono<CheckoutOrderResponse> getOrder(@PathVariable UUID id) {
    return service.getOrder(id);
  }

  @GetMapping("/orders")
  public Mono<PageResponse<CheckoutOrderResponse>> getOrders(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "" + ApiConstants.DEFAULT_PAGE_SIZE) int size) {
    return service.getOrders(jwt.getSubject(), page, Math.min(size, ApiConstants.MAX_PAGE_SIZE));
  }
}
