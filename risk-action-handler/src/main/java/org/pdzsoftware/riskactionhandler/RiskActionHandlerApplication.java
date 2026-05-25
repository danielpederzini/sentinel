package org.pdzsoftware.riskactionhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RiskActionHandlerApplication {

	static void main(String[] args) {
		SpringApplication.run(RiskActionHandlerApplication.class, args);
	}

}
