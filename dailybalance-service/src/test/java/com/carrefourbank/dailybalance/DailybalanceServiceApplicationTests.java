package com.carrefourbank.dailybalance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // Adicione esta linha para usar o perfil de teste
class DailybalanceServiceApplicationTests {

	@Test
	void contextLoads() {
		// Teste básico para verificar se o contexto da aplicação carrega
	}
}