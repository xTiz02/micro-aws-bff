package com.prd.bffclient;

import com.prd.bffclient.config.ClientsProperties;
import com.prd.common.logging.CorrelationIdSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClientsProperties.class)
public class BffClientApplication {

  public static void main(String[] args) {
    CorrelationIdSupport.enable();
    SpringApplication.run(BffClientApplication.class, args);
  }

}
