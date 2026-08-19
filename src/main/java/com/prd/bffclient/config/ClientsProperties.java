package com.prd.bffclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record ClientsProperties(Client catalogService, Client orderService) {

  public record Client(String baseUrl, long timeoutMs) {
  }
}
