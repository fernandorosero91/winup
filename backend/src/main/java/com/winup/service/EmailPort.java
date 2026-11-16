package com.winup.service;

import java.math.BigDecimal;

public interface EmailPort {
    void sendWin(String to, String displayName, String matchName, BigDecimal winnings);

    void sendLoss(String to, String displayName, String matchName, BigDecimal lost);

    void sendPromotion(String to, String displayName, String title, String detail);
}
