package com.chaekchaek.incident.analysis.service;

import com.chaekchaek.incident.analysis.domain.ModelCall;
import com.chaekchaek.incident.analysis.repository.ModelCallRepository;
import com.chaekchaek.incident.config.IncidentProperties;
import java.time.Clock;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelBudgetService {
    private final ModelCallRepository calls;
    private final IncidentProperties properties;
    private final Clock clock;

    @Transactional
    public boolean reserve() {
        var now = clock.instant();
        var start = now.atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate()
                .atStartOfDay(ZoneId.of("Asia/Seoul"))
                .toInstant();
        if (!properties.llmEnabled() || calls.countByReservedAtGreaterThanEqual(start) >= properties.dailyCallLimit()) {
            return false;
        }
        calls.save(new ModelCall(now));
        return true;
    }
}
