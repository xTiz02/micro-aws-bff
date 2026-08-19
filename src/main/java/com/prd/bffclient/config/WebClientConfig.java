package com.prd.bffclient.config;

import com.prd.common.constant.SecurityConstants;
import com.prd.common.logging.CorrelationIdSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * catalog-service y order-service no son alcanzables por el cliente externo (ver README:
 * el ALB solo expone el BFF). El BFF valida el JWT una vez en el borde y lo reenvía a los
 * servicios internos, que lo vuelven a validar como resource servers propios
 * (defensa en profundidad, no solo perímetro).
 */
@Configuration
public class WebClientConfig {

  @Bean
  public WebClient catalogWebClient(ClientsProperties properties) {
    return WebClient.builder()
        .baseUrl(properties.catalogService().baseUrl())
        .filter(propagateAuthorization())
        .filter(propagateCorrelationId())
        .build();
  }

  @Bean
  public WebClient orderWebClient(ClientsProperties properties) {
    return WebClient.builder()
        .baseUrl(properties.orderService().baseUrl())
        .filter(propagateAuthorization())
        .filter(propagateCorrelationId())
        .build();
  }

  private ExchangeFilterFunction propagateAuthorization() {
    return (request, next) -> ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .cast(JwtAuthenticationToken.class)
        .map(auth -> ClientRequest.from(request)
            .headers(headers -> headers.setBearerAuth(auth.getToken().getTokenValue()))
            .build())
        .defaultIfEmpty(request)
        .flatMap(next::exchange);
  }

  /** Reenvía el mismo correlationId que CorrelationIdWebFilter puso en el Reactor Context
   * al entrar al BFF, para que catalog-service/order-service logueen bajo el mismo id
   * y todo el rastro de una request quede unido entre los tres servicios. */
  private ExchangeFilterFunction propagateCorrelationId() {
    return (request, next) -> Mono.deferContextual(ctx -> {
      String correlationId = ctx.getOrDefault(CorrelationIdSupport.MDC_KEY, null);
      ClientRequest outgoing = correlationId == null
          ? request
          : ClientRequest.from(request)
              .header(SecurityConstants.CORRELATION_ID_HEADER, correlationId)
              .build();
      return next.exchange(outgoing);
    });
  }
}
