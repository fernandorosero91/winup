package com.winup.service.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentReturnPage {
    private static final String APP_LINK = "winup://payment/return";

    private static final String PAGE = """
            <!doctype html>
            <html lang="es">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>WINUP</title>
            <style>
            :root { color-scheme: dark; }
            * { box-sizing: border-box; }
            body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: #101B2B; color: #F8F9FA; font-family: Inter, system-ui, sans-serif; }
            main { width: min(420px, 92vw); padding: 32px 24px; border-radius: 20px; background: #141B2D; border: 1px solid #24304A; text-align: center; }
            h1 { margin: 0 0 8px; font-size: 22px; }
            p { margin: 0 0 24px; font-size: 14px; line-height: 1.6; color: #94A3B8; }
            a { display: block; padding: 14px 20px; border-radius: 12px; background: #39FF14; color: #0F172A; font-size: 15px; font-weight: 700; text-decoration: none; }
            small { display: block; margin-top: 16px; font-size: 12px; color: #64748B; }
            </style>
            </head>
            <body>
            <main>
            <h1>Pago procesado</h1>
            <p>Vuelve a WINUP para ver tu saldo actualizado. Cada segundo importa.</p>
            <a href="$TARGET">Continuar en WINUP</a>
            <small>Si el botón no te lleva de vuelta, abre WINUP manualmente.</small>
            </main>
            <script>
            window.setTimeout(function () { window.location.replace("$TARGET"); }, 700);
            </script>
            </body>
            </html>
            """;

    public String render(String redirectTo) {
        String target = redirectTo == null || redirectTo.isBlank() ? APP_LINK : redirectTo;
        return PAGE.replace("$TARGET", escape(target));
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
