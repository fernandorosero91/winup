package com.winup.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PromoDispatcher {
    private final PromoService promoService;

    public PromoDispatcher(PromoService promoService) {
        this.promoService = promoService;
    }

    @Scheduled(fixedDelayString = "${winup.promo.interval-ms:15000}")
    public void dispatch() {
        promoService.dispatchDue();
    }
}
