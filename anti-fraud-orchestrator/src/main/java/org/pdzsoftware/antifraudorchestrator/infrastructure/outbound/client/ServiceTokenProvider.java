package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client;

import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.ServiceAuthProperties;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Mints short-lived RS256 JWTs that authenticate this service when calling downstream
 * internal services. Tokens are cached and reused until shortly before they expire.
 */
@Component
public class ServiceTokenProvider {
	private static final long REFRESH_SKEW_SECONDS = 10;

	private final JwtEncoder jwtEncoder;
	private final ServiceAuthProperties properties;

	private String cachedToken;
	private Instant cachedTokenExpiresAt = Instant.EPOCH;

	public ServiceTokenProvider(JwtEncoder jwtEncoder, ServiceAuthProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public synchronized String getToken() {
		Instant now = Instant.now();
		if (cachedToken != null && now.isBefore(cachedTokenExpiresAt.minusSeconds(REFRESH_SKEW_SECONDS))) {
			return cachedToken;
		}

		Instant expiresAt = now.plusSeconds(properties.getTokenTtlSeconds());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getIssuer())
				.subject(properties.getIssuer())
				.audience(List.of(properties.getAudience()))
				.issuedAt(now)
				.expiresAt(expiresAt)
				.build();

		cachedToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		cachedTokenExpiresAt = expiresAt;
		return cachedToken;
	}
}
