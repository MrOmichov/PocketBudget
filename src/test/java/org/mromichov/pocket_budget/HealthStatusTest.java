package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.Mockito.when;

@WebMvcTest(HealthController.class)
@AutoConfigureRestTestClient
public class HealthStatusTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private HealthService healthService;

    @Test
    void healthStatusShouldReturnUpMessage() {
        when(healthService.status()).thenReturn(new HealthStatus("UP"));

        restTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus()
                .is2xxSuccessful();

        restTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectBody(HealthStatus.class)
                .isEqualTo(new HealthStatus("UP"));
    }

}
