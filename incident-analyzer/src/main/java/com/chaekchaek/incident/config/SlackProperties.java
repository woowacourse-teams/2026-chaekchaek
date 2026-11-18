package com.chaekchaek.incident.config;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("incident.slack")
public record SlackProperties(
        boolean enabled,
        String appToken,
        String botToken,
        String channelDev,
        String channelProd,
        String alertBotIdDev,
        String alertBotIdProd,
        URI apiUrl
) {
    public SlackProperties {
        if (enabled && (!appToken.startsWith("xapp-") || !botToken.startsWith("xoxb-"))) {
            throw new IllegalArgumentException("Slack 활성화에는 앱 토큰과 봇 토큰이 필요합니다");
        }
        if (enabled && channelDev.isBlank() && channelProd.isBlank()) {
            throw new IllegalArgumentException("Slack 분석 채널을 지정해야 합니다");
        }
        validateChannel(channelDev, alertBotIdDev);
        validateChannel(channelProd, alertBotIdProd);
        if (!channelDev.isBlank() && channelDev.equals(channelProd)) {
            throw new IllegalArgumentException("개발과 운영 분석 채널은 달라야 합니다");
        }
    }

    private static void validateChannel(String channel, String botId) {
        if (!channel.isBlank() && (!channel.matches("[CG][A-Z0-9]+") || !botId.matches("B[A-Z0-9]+"))) {
            throw new IllegalArgumentException("채널 ID와 원본 알림의 봇 ID가 필요합니다");
        }
    }

    public Optional<String> environment(String channel) {
        if (!channelDev.isBlank() && channelDev.equals(channel)) {
            return Optional.of("dev");
        }
        if (!channelProd.isBlank() && channelProd.equals(channel)) {
            return Optional.of("prod");
        }
        return Optional.empty();
    }

    public Set<String> channels() {
        return Stream.of(channelDev, channelProd)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String alertBotId(String environment) {
        return switch (environment) {
            case "dev" -> alertBotIdDev;
            case "prod" -> alertBotIdProd;
            default -> throw new IllegalArgumentException("허용된 환경이 필요합니다");
        };
    }

    @Override
    public String toString() {
        return "SlackProperties[enabled=" + enabled + ", channels=" + channels() + "]";
    }
}
