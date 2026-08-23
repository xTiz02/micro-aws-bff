package com.prd.bffclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain filterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/actuator/health/**", "/ping").permitAll()
            .pathMatchers("/checkout/**").authenticated()
            .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
        .build();
  }

  /**
   * Bean explícito (en vez de dejar que Boot autoconfigure uno a partir de issuer-uri) para
   * poder inyectarle el mismo HttpClient con el resolver DNS de la JVM (ver WebClientConfig):
   * sin esto, el descubrimiento OIDC y la descarga de JWKS contra oauth-server fallan con
   * UnknownHostException al usar el nombre corto interno de Service Connect.
   */
  @Bean
  public ReactiveJwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      HttpClient internalHttpClient) {
    WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(internalHttpClient))
        .build();
    return NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri)
        .webClient(webClient)
        .build();
  }
}
