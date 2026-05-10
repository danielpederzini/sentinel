package org.pdzsoftware.antifraudorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class AntiFraudOrchestratorApplication {

	static void main(String[] args) {
		SpringApplication.run(AntiFraudOrchestratorApplication.class, args);
	}

}
