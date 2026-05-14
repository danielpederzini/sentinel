package org.pdzsoftware.featuremanager.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repostiory.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Cacheable(cacheNames = "userAverageAmounts", key = "#a0", condition = "#a0 != null")
    public BigDecimal findAverageAmountByUserId(String userId) {
        Double average = transactionRepository.findAverageAmountByUserId(userId);
        return average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average);
    }

    @Cacheable(cacheNames = "ipRiskScores", key = "#a0", condition = "#a0 != null && !#a0.isBlank()")
    public float findIpRiskScoreByIpAddress(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return 0F;
        }

        Double ipRiskScore = transactionRepository.findIpRiskScoreByIpAddress(ipAddress);
        return ipRiskScore == null ? 0F : ipRiskScore.floatValue();
    }
}
