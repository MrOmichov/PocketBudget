package org.mromichov.pocket_budget.health;

import org.springframework.stereotype.Service;

@Service
public class HealthService {
    public HealthStatus status() {
        return new HealthStatus("UP");
    }
}
