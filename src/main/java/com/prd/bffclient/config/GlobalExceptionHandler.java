package com.prd.bffclient.config;

import com.prd.common.dto.ApiError;
import com.prd.common.exception.BusinessException;
import com.prd.common.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public Mono<ResponseEntity<ApiError>> handleNotFound(ResourceNotFoundException ex, ServerHttpRequest request) {
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of("NOT_FOUND", ex.getMessage(), request.getPath().value())));
  }

  @ExceptionHandler(BusinessException.class)
  public Mono<ResponseEntity<ApiError>> handleBusiness(BusinessException ex, ServerHttpRequest request) {
    return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ApiError.of(ex.getCode(), ex.getMessage(), request.getPath().value())));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ApiError>> handleValidation(WebExchangeBindException ex, ServerHttpRequest request) {
    String message = ex.getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .reduce((a, b) -> a + "; " + b)
        .orElse("Solicitud inválida");
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of("VALIDATION_ERROR", message, request.getPath().value())));
  }

  /** El circuit breaker de catalog-service u order-service está abierto: el downstream
   * viene fallando y dejamos de insistir en vez de acumular más carga sobre un servicio caído. */
  @ExceptionHandler(CallNotPermittedException.class)
  public Mono<ResponseEntity<ApiError>> handleCircuitOpen(CallNotPermittedException ex, ServerHttpRequest request) {
    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiError.of("SERVICE_UNAVAILABLE", "El servicio no está disponible en este momento", request.getPath().value())));
  }

  @ExceptionHandler(TimeoutException.class)
  public Mono<ResponseEntity<ApiError>> handleTimeout(TimeoutException ex, ServerHttpRequest request) {
    return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
        .body(ApiError.of("GATEWAY_TIMEOUT", "El servicio tardó demasiado en responder", request.getPath().value())));
  }

  @ExceptionHandler(WebClientResponseException.class)
  public Mono<ResponseEntity<ApiError>> handleDownstreamError(WebClientResponseException ex, ServerHttpRequest request) {
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ApiError.of("DOWNSTREAM_ERROR", "Error al consultar un servicio interno", request.getPath().value())));
  }
}
