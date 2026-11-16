package com.winup.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class WinUpEmailTemplate {
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final String publicUrl;

    public WinUpEmailTemplate(@Value("${winup.public-url:http://localhost:4200}") String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String win(String displayName, String matchName, BigDecimal winnings) {
        return render(new Layout(
                "¡Ganaste!",
                "APUESTA GANADA",
                displayName,
                matchName,
                winnings,
                "Ganancia acreditada",
                "#15803D",
                "#EFFDE8",
                "#BBF7A8",
                "El importe ya está disponible en tu cartera WINUP.",
                "#39FF14",
                "Ver mi cartera"
        ));
    }

    public String loss(String displayName, String matchName, BigDecimal lost) {
        return render(new Layout(
                "Esta vez no fue",
                "APUESTA PERDIDA",
                displayName,
                matchName,
                lost,
                "Importe perdido",
                "#B42318",
                "#FFF1F1",
                "#FECACA",
                "No se acreditó nada en tu cartera esta vez. El importe quedó registrado en tu historial.",
                "#FF5C5C",
                "Revisar mis apuestas"
        ));
    }

    public String promotion(String displayName, String title, String detail) {
        return render(new Layout(
                title,
                "PROMOCIÓN WINUP",
                displayName,
                null,
                null,
                null,
                null,
                null,
                null,
                detail,
                "#FF8A00",
                "Aprovechar promoción"
        ));
    }

    private String render(Layout layout) {
        String match = layout.matchName() == null
                ? ""
                : "<tr><td style=\"padding:0 0 16px;color:#475569;font-size:15px;\">"
                + escape(layout.matchName()) + "</td></tr>";
        String amount = layout.amount() == null ? "" : amountBlock(layout);

        return "<!doctype html><html lang=\"es\"><body style=\"margin:0;padding:24px;background:#F1F5F9;"
                + "font-family:Arial,Helvetica,sans-serif;color:#0F172A;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:620px;margin:0 auto;background:#FFFFFF;border-radius:20px;"
                + "border:1px solid #E2E8F0;overflow:hidden;\">"
                + "<tr><td style=\"padding:26px 32px;background:#0B1020;\">"
                + "<img src=\"cid:winup-logo\" width=\"96\" height=\"71\" alt=\"WINUP\" "
                + "style=\"display:block;border:0;\" />"
                + "<p style=\"margin:16px 0 0;color:#94A3B8;font-size:12px;letter-spacing:2px;\">"
                + "CADA SEGUNDO IMPORTA</p></td></tr>"
                + "<tr><td style=\"padding:32px 32px 8px;\">"
                + "<p style=\"margin:0 0 6px;color:" + layout.accent() + ";font-size:12px;font-weight:700;"
                + "letter-spacing:1.5px;text-transform:uppercase;\">" + escape(layout.eyebrow()) + "</p>"
                + "<h1 style=\"margin:0 0 18px;font-size:26px;line-height:1.2;\">" + escape(layout.title()) + "</h1>"
                + "<p style=\"margin:0 0 18px;color:#475569;font-size:15px;\">Hola "
                + escape(layout.displayName()) + ",</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + match
                + amount
                + "<tr><td style=\"padding:0 0 26px;color:#334155;font-size:15px;line-height:1.6;\">"
                + escape(layout.message()) + "</td></tr>"
                + "<tr><td style=\"padding:0 0 30px;\"><a href=\"" + escape(publicUrl) + "\" "
                + "style=\"display:inline-block;padding:14px 24px;border-radius:12px;background:"
                + layout.accent() + ";color:#0B1020;font-weight:700;text-decoration:none;\">"
                + escape(layout.button()) + "</a></td></tr>"
                + "</table></td></tr>"
                + "<tr><td style=\"padding:20px 32px;background:#F8FAFC;border-top:1px solid #E2E8F0;"
                + "color:#64748B;font-size:12px;line-height:1.6;\">"
                + "WINUP · Plataforma académica de apuestas deportivas en vivo. "
                + "Los partidos son simulados; el dinero es ficticio.<br />"
                + "Si no reconoces esta actividad, ignora este mensaje.</td></tr>"
                + "</table></body></html>";
    }

    private String amountBlock(Layout layout) {
        return "<tr><td style=\"padding:0 0 20px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:" + layout.amountBackground() + ";border:1px solid "
                + layout.amountBorder() + ";border-radius:14px;\">"
                + "<tr><td style=\"padding:16px 20px;\">"
                + "<p style=\"margin:0 0 4px;color:#64748B;font-size:11px;font-weight:700;"
                + "letter-spacing:1.4px;text-transform:uppercase;\">" + escape(layout.amountLabel()) + "</p>"
                + "<p style=\"margin:0;color:" + layout.amountColor() + ";font-size:26px;font-weight:700;"
                + "font-family:'Courier New',monospace;\">" + money(layout.amount()) + "</p>"
                + "</td></tr></table></td></tr>";
    }

    private String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(COLOMBIA);
        return "$ " + new DecimalFormat("#,##0.00", symbols).format(amount);
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record Layout(
            String title,
            String eyebrow,
            String displayName,
            String matchName,
            BigDecimal amount,
            String amountLabel,
            String amountColor,
            String amountBackground,
            String amountBorder,
            String message,
            String accent,
            String button
    ) {
    }
}
