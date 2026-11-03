package org.app.autfmi.util;

import jakarta.mail.MessagingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Identidad común de las notificaciones, independiente del transporte SMTP/Graph. */
public final class MailBranding {
    private static final String CID = "fractal-notification-logo";
    private static final String AVISO = "Este correo ha sido enviado automáticamente desde FRACTAL. "
            + "Esta es una bandeja DESATENDIDA; por favor, no respondas a este mensaje.";

    private MailBranding() { }

    public static void apply(MimeMessageHelper helper, String content, boolean html) throws MessagingException {
        Document document = html ? Jsoup.parse(content == null ? "" : content) : Jsoup.parse("");
        document.outputSettings().prettyPrint(false);
        if (!html) document.body().appendElement("div").attr("style", "white-space:pre-wrap")
                .text(content == null ? "" : content);
        if (document.getElementById("fractal-notification-header") == null) {
            document.body().prepend("<div id='fractal-notification-header' style='padding:16px;background:#ffffff'>"
                    + "<img src='cid:" + CID + "' alt='FRACTAL' width='220' style='width:220px;max-width:100%;height:auto'>"
                    + "</div>");
            document.body().appendElement("p").attr("id", "fractal-notification-footer")
                    .attr("style", "padding:16px;border-top:1px solid #ddd;font:12px Arial,sans-serif;color:#555")
                    .text(AVISO);
        }
        helper.setText(document.outerHtml(), true);
        helper.addInline(CID, new ClassPathResource("assets/fractal-notification-logo.png"), "image/png");
    }
}