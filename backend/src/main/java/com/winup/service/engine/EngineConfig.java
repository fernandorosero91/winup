package com.winup.service.engine;

import com.winup.model.match.DecimalPayoutStrategy;
import com.winup.model.match.PayoutStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {
    @Bean
    public PayoutStrategy payoutStrategy() {
        return new DecimalPayoutStrategy();
    }

    @Bean
    public MatchEngine matchEngine(PayoutStrategy payoutStrategy) {
        return new MatchEngine(payoutStrategy);
    }
}
