package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.expenses.ExpensesController;
import org.mromichov.pocket_budget.health.HealthController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PocketBudgetApplicationTests {
	@Autowired
	private HealthController healthController;

	@Autowired
	private ExpensesController expensesController;

	@Test
	void contextLoads() {
		assertThat(healthController).isNotNull();
		assertThat(expensesController).isNotNull();
	}

}
