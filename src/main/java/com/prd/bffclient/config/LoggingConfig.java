package com.prd.bffclient.config;

import com.prd.common.web.CorrelationIdWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

@Configuration
public class LoggingConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public WebFilter correlationIdWebFilter() {
    return new CorrelationIdWebFilter();
  }
}
