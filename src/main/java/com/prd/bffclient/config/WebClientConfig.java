package com.prd.bffclient.config;

import com.prd.common.constant.SecurityConstants;
import com.prd.common.logging.CorrelationIdSupport;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * catalog-service y order-service no son alcanzables por el cliente externo (ver README:
 * el ALB solo expone el BFF). El BFF valida el JWT una vez en el borde y lo reenvía a los
 * servicios internos, que lo vuelven a validar como resource servers propios
 * (defensa en profundidad, no solo perímetro).
 */
@Configuration
public class WebClientConfig {

  /**
   * El resolver DNS asíncrono por defecto de reactor-netty no respeta de forma confiable
   * las entradas que ECS Service Connect inyecta para resolver los nombres cortos internos
   * (oauth-server, catalog-server, order-server) — problema documentado en reactor-netty
   * al combinarse con Service Connect. Forzamos el resolver nativo de la JVM, que sí los
   * respeta correctamente.
   */
  @Bean
  public HttpClient internalHttpClient() {
    return HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE);
  }

  @Bean
  public WebClient catalogWebClient(ClientsProperties properties, HttpClient internalHttpClient) {
    return WebClient.builder()
        .baseUrl(properties.catalogService().baseUrl())
        .clientConnector(new ReactorClientHttpConnector(internalHttpClient))
        .filter(propagateAuthorization())
        .filter(propagateCorrelationId())
        .build();
  }

  @Bean
  public WebClient orderWebClient(ClientsProperties properties, HttpClient internalHttpClient) {
    return WebClient.builder()
        .baseUrl(properties.orderService().baseUrl())
        .clientConnector(new ReactorClientHttpConnector(internalHttpClient))
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
