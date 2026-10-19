package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.health.HealthController;
import org.mromichov.pocket_budget.health.HealthService;
import org.mromichov.pocket_budget.health.HealthStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(HealthStatus.class).isEqualTo(new HealthStatus("UP"));
    }

}
