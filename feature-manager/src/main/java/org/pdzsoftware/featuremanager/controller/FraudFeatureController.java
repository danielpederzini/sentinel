package org.pdzsoftware.featuremanager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.dto.FraudFeatureResult;
import org.pdzsoftware.featuremanager.usecase.CalculateFraudFeaturesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-features")
@RequiredArgsConstructor
public class FraudFeatureController {
    private final CalculateFraudFeaturesUseCase calculateFraudFeaturesUseCase;

    @PostMapping
    public ResponseEntity<FraudFeatureResult> calculateFraudFeatures(@RequestBody @Valid FraudFeatureRequest request) {
        FraudFeatureResult result = calculateFraudFeaturesUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
