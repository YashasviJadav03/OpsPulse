package com.opspulse.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn401WhenCallingProtectedApiWithoutToken() {
        webTestClient.get()
                .uri("/api/monitors")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenCallingProtectedApiWithTamperedToken() {
        webTestClient.get()
                .uri("/api/monitors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.tampered.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
