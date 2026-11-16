package com.winup.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailAdapterTest {
    @Test
    void should_render_the_win_template_with_the_brand_logo_and_amount() {
        WinUpEmailTemplate template = new WinUpEmailTemplate("http://localhost:4200");

        String html = template.win("Alex Martínez", "Gen.G vs T1", new BigDecimal("9250.00"));

        assertThat(html).contains("cid:winup-logo");
        assertThat(html).contains("¡Ganaste!");
        assertThat(html).contains("Gen.G vs T1");
        assertThat(html).contains("$ 9.250,00");
        assertThat(html).contains("CADA SEGUNDO IMPORTA");
    }

    @Test
    void should_render_the_loss_template_as_a_loss_not_a_prize() {
        WinUpEmailTemplate template = new WinUpEmailTemplate("http://localhost:4200");

        String html = template.loss("FErnando Rosero", "Aalborg vs Barcelona", new BigDecimal("50000.00"));

        assertThat(html).contains("APUESTA PERDIDA");
        assertThat(html).contains("Esta vez no fue");
        assertThat(html).contains("Importe perdido");
        assertThat(html).contains("$ 50.000,00");
        assertThat(html).contains("No se acreditó nada");
        assertThat(html).contains("#B42318");
        assertThat(html).doesNotContain("¡Ganaste!");
    }

    @Test
    void should_escape_user_content_in_the_template() {
        WinUpEmailTemplate template = new WinUpEmailTemplate("http://localhost:4200");

        String html = template.promotion("Alex", "<script>alert(1)</script>", "Bono <b>20%</b>");

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("Bono &lt;b&gt;20%&lt;/b&gt;");
    }

    @Test
    void should_build_a_multipart_email_with_the_inline_logo() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        SmtpEmailAdapter adapter = new SmtpEmailAdapter(
                mailSender,
                new WinUpEmailTemplate("http://localhost:4200"),
                "WINUP <no-reply@winup.app>",
                "demo@gmail.com",
                true
        );

        adapter.sendWin("destino@winup.app", "Alex", "Gen.G vs T1", new BigDecimal("9250.00"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();
        assertThat(sent.getSubject()).contains("Ganaste");
        assertThat(sent.getContentType()).containsIgnoringCase("multipart");
        assertThat(containsLogo((jakarta.mail.Part) sent)).isTrue();
    }

    private boolean containsLogo(jakarta.mail.Part part) throws Exception {
        String[] contentId = part.getHeader("Content-ID");
        if (contentId != null && contentId.length > 0 && contentId[0].contains("winup-logo")) {
            return true;
        }
        if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                if (containsLogo(multipart.getBodyPart(index))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void should_skip_sending_when_smtp_credentials_are_missing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailAdapter adapter = new SmtpEmailAdapter(
                mailSender,
                new WinUpEmailTemplate("http://localhost:4200"),
                "WINUP <no-reply@winup.app>",
                "",
                true
        );

        adapter.sendWin("demo@winup.app", "Alex", "Gen.G vs T1", new BigDecimal("9250.00"));

        verify(mailSender, never()).createMimeMessage();
    }
}
