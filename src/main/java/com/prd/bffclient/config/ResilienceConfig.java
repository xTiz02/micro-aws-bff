package com.prd.bffclient.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .slidingWindowSize(10)
        .minimumNumberOfCalls(5)
        .waitDurationInOpenState(Duration.ofSeconds(10))
        .permittedNumberOfCallsInHalfOpenState(3)
        .build();
    return CircuitBreakerRegistry.of(config);
  }

  @Bean
  public CircuitBreaker catalogCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("catalog-service");
  }

  @Bean
  public CircuitBreaker orderCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("order-service");
  }
}
