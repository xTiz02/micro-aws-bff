package com.prd.bffclient.web;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CheckoutControllerIT {

  private static WireMockServer catalogMock;
  private static WireMockServer orderMock;

  @BeforeAll
  static void startMocks() {
    catalogMock = new WireMockServer(0);
    catalogMock.start();
    orderMock = new WireMockServer(0);
    orderMock.start();
  }

  @AfterAll
  static void stopMocks() {
    catalogMock.stop();
    orderMock.stop();
  }

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("app.clients.catalog-service.base-url", () -> "http://localhost:" + catalogMock.port());
    registry.add("app.clients.order-service.base-url", () -> "http://localhost:" + orderMock.port());
  }

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void checkoutComputesTotalFromCatalogAndEnrichesTheResponse() {
    UUID productId = UUID.randomUUID();
    catalogMock.stubFor(get(urlEqualTo("/products/" + productId))
        .willReturn(okJson("""
            {"id":"%s","name":"Teclado","description":"d","category":"c","price":10.00,"stock":5,"active":true}
            """.formatted(productId))));

    UUID orderId = UUID.randomUUID();
    orderMock.stubFor(post(urlEqualTo("/orders"))
        .willReturn(okJson("""
            {"id":"%s","customerId":"client","status":"CREATED","totalAmount":20.00,
             "items":[{"productId":"%s","quantity":2,"unitPrice":10.00,"lineTotal":20.00}],
             "createdAt":"2026-01-01T00:00:00Z"}
            """.formatted(orderId, productId))));

    webTestClient
        .mutateWith(mockJwt().jwt(jwt -> jwt.subject("client")))
        .post().uri("/checkout/orders")
        .header("Content-Type", "application/json")
        .bodyValue("""
            {"items":[{"productId":"%s","quantity":2}]}
            """.formatted(productId))
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.totalAmount").isEqualTo(20.00)
        .jsonPath("$.items[0].productName").isEqualTo("Teclado");
  }

  @Test
  void rejectsCheckoutWhenStockIsInsufficient() {
    UUID productId = UUID.randomUUID();
    catalogMock.stubFor(get(urlEqualTo("/products/" + productId))
        .willReturn(okJson("""
            {"id":"%s","name":"Mouse","description":"d","category":"c","price":5.00,"stock":1,"active":true}
            """.formatted(productId))));

    webTestClient
        .mutateWith(mockJwt().jwt(jwt -> jwt.subject("client")))
        .post().uri("/checkout/orders")
        .header("Content-Type", "application/json")
        .bodyValue("""
            {"items":[{"productId":"%s","quantity":5}]}
            """.formatted(productId))
        .exchange()
        .expectStatus().isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  void rejectsRequestWithoutToken() {
    webTestClient
        .get().uri("/checkout/products")
        .exchange()
        .expectStatus().isUnauthorized();
  }
}
