package org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "JWKS", description = "Public keys for verifying service-to-service JWTs")
public class JwksController {
	private final RSAKey rsaKey;

	public JwksController(RSAKey rsaKey) {
		this.rsaKey = rsaKey;
	}

	@Operation(
			summary = "Get the JSON Web Key Set",
			description = """
					Returns the public RSA keys (in JWKS format) that downstream services use to verify \
					JWTs minted by this orchestrator. Keys are generated in memory at startup.""")
	@ApiResponse(responseCode = "200", description = "JSON Web Key Set")
	@GetMapping("/.well-known/jwks.json")
	public Map<String, Object> jwks() {
		return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
	}
}
