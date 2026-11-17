package com.chaekchaek.incident.config;

import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("incident")
public record IncidentProperties(
        String webhookToken,
        long orgId,
        Set<String> enabledEnvironments,
        URI lokiUrl,
        URI prometheusUrl,
        URI grafanaUrl,
        URI llmUrl,
        String llmApiKey,
        String llmModel,
        boolean llmEnabled,
        int dailyCallLimit
) {
    public IncidentProperties {
        enabledEnvironments = enabledEnvironments == null ? Set.of() : Set.copyOf(enabledEnvironments);
        if (!Set.of("dev", "prod").containsAll(enabledEnvironments)) {
            throw new IllegalArgumentException("분석 환경은 dev, prod 중에서 선택해야 합니다");
        }
        if (!enabledEnvironments.isEmpty() && (webhookToken == null || webhookToken.length() < 32)) {
            throw new IllegalArgumentException("웹훅 인증값은 32자 이상이어야 합니다");
        }
        if (llmEnabled && (llmApiKey.isBlank() || llmModel.isBlank() || dailyCallLimit < 1)) {
            throw new IllegalArgumentException("LLM 활성화에는 키, 모델, 일일 호출 한도가 필요합니다");
        }
    }
}
