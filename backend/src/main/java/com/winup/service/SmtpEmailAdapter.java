package com.winup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class SmtpEmailAdapter implements EmailPort {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final WinUpEmailTemplate template;
    private final String from;
    private final String username;
    private final boolean enabled;

    public SmtpEmailAdapter(
            JavaMailSender mailSender,
            WinUpEmailTemplate template,
            @Value("${winup.mail.from:WINUP <no-reply@winup.app>}") String from,
            @Value("${spring.mail.username:}") String username,
            @Value("${winup.mail.enabled:true}") boolean enabled
    ) {
        this.mailSender = mailSender;
        this.template = template;
        this.from = from;
        this.username = username;
        this.enabled = enabled;
    }

    @Override
    public void sendWin(String to, String displayName, String matchName, BigDecimal winnings) {
        send(to, "Ganaste " + money(winnings) + " en WINUP",
                template.win(displayName, matchName, winnings));
    }

    @Override
    public void sendLoss(String to, String displayName, String matchName, BigDecimal lost) {
        send(to, "Resultado de tu apuesta en WINUP",
                template.loss(displayName, matchName, lost));
    }

    @Override
    public void sendPromotion(String to, String displayName, String title, String detail) {
        send(to, "WINUP · " + title, template.promotion(displayName, title, detail));
    }

    private void send(String to, String subject, String html) {
        if (!enabled || username == null || username.isBlank()) {
            log.info("SMTP sin credenciales; correo para {} omitido", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addInline("winup-logo", new ClassPathResource("email/winup-mark.png"), "image/png");
            mailSender.send(message);
            log.info("Correo enviado a {} con asunto {}", to, subject);
        } catch (MessagingException | MailException exception) {
            log.error("No se pudo enviar el correo a {}", to, exception);
        }
    }

    private String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
        return "$ " + new DecimalFormat("#,##0.00", symbols).format(amount);
    }
}
