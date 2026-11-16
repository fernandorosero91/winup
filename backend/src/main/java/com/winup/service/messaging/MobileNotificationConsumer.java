package com.winup.service.messaging;

import com.winup.config.RabbitConfig;
import com.winup.service.MobilePushPort;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

@Component
public class MobileNotificationConsumer {
    private final MobilePushPort mobilePushPort;

    public MobileNotificationConsumer(MobilePushPort mobilePushPort) {
        this.mobilePushPort = mobilePushPort;
    }

    @RabbitListener(queues = RabbitConfig.MOBILE_QUEUE)
    public void consume(Map<String, Object> payload) {
        if ("PROMO".equals(payload.get("type"))) {
            mobilePushPort.push(
                    "todos",
                    text(payload.get("title")),
                    text(payload.get("detail"))
            );
            return;
        }
        boolean won = "WIN".equals(payload.get("result"));
        Object amount = won ? payload.get("winnings") : payload.get("lost");
        mobilePushPort.push(
                text(payload.get("userId")),
                won ? "Apuesta ganada" : "Apuesta perdida",
                text(payload.get("matchName")) + (won ? " · ganaste " : " · perdiste ") + money(amount)
        );
    }

    private String text(Object value) {
        return value == null ? "Partido" : value.toString();
    }

    private String money(Object value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
        return "$ " + new DecimalFormat("#,##0.00", symbols).format(amount);
    }
}
