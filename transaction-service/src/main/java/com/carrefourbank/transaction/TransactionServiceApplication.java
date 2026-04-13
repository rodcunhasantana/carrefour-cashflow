// TransactionServiceApplication.java
package com.carrefourbank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal para iniciar o serviço de transações.
 */
@SpringBootApplication
public class TransactionServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(TransactionServiceApplication.class, args);
	}
}