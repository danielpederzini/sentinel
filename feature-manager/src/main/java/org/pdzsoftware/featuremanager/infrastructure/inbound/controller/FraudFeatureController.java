package org.pdzsoftware.featuremanager.infrastructure.inbound.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.ErrorResponse;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureResponse;
import org.pdzsoftware.featuremanager.application.usecase.CalculateFraudFeaturesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-features")
@RequiredArgsConstructor
@Tag(name = "Fraud Features", description = "Real-time computation of fraud features for a transaction")
public class FraudFeatureController {
    private final CalculateFraudFeaturesUseCase calculateFraudFeaturesUseCase;

    @Operation(
            summary = "Calculate fraud features",
            description = "Computes the full set of fraud features for the given transaction, using cached velocity and profile data.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Features calculated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT bearer token", content = @Content)
    })
    @PostMapping
    public ResponseEntity<FraudFeatureResponse> calculateFraudFeatures(@RequestBody @Valid FraudFeatureRequest request) {
        FraudFeatureResponse result = calculateFraudFeaturesUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
