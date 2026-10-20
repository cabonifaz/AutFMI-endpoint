package org.app.autfmi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.ContactoTalentoDTO;
import org.springframework.web.util.HtmlUtils;

/**
 * Piezas HTML compartidas por los correos de formularios.
 */
public final class FormularioCorreoUtils {

    private FormularioCorreoUtils() {
    }

    private static final String TH = "style=\"background-color:#2D5294;color:#fff;padding:5px;text-align:center;\"";
    private static final String TD = "style=\"padding:5px;text-align:center;\"";

    // Casillas marcada / vacía como entidades HTML (☑ / ☐) y no <input type="checkbox">:
    // los clientes de correo (Gmail, Outlook) eliminan los controles de formulario.
    // En Thymeleaf se pintan con th:utext.
    private static final String CASILLA_MARCADA = "&#9745;";
    private static final String CASILLA_VACIA = "&#9744;";

    /**
     * Requisitos de ingreso del RQ, en orden: nombre, "Sí"|"No" y la casilla
     * (☑ requiere / ☐ no requiere). Se usa en la plantilla rq-details.
     */
    public static List<Map<String, String>> requisitosIngreso(Integer emo, Integer sctr, Integer asignacionLaptop) {
        List<Map<String, String>> requisitos = new ArrayList<>();
        requisitos.add(requisito("EMO", emo));
        requisitos.add(requisito("SCTR", sctr));
        requisitos.add(requisito("Asignación de laptop", asignacionLaptop));
        return requisitos;
    }

    private static Map<String, String> requisito(String nombre, Integer valor) {
        boolean requiere = Integer.valueOf(1).equals(valor);
        return Map.of("nombre", nombre, "valor", siNo(valor), "casilla", requiere ? CASILLA_MARCADA : CASILLA_VACIA);
    }

    /** Bloque HTML con los requisitos de ingreso del RQ (correo "Ingreso de nuevo talento"). */
    public static String requisitosIngresoHtml(Integer emo, Integer sctr, Integer asignacionLaptop) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" style=\"width:auto;border-collapse:collapse;\">")
                .append("<tr><th ").append(TH).append(" colspan=\"2\">Requisitos de ingreso</th></tr>");
        for (Map<String, String> r : requisitosIngreso(emo, sctr, asignacionLaptop)) {
            sb.append("<tr><td style=\"padding:5px;text-align:left;\">").append(HtmlUtils.htmlEscape(r.get("nombre")))
                    .append("</td><td style=\"padding:5px;text-align:center;font-size:18px;line-height:1;\">")
                    .append(r.get("casilla")).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    /** Tabla con los datos de contacto de los talentos (nombres, apellidos, correo, celular). */
    public static String tablaContactoHtml(List<ContactoTalentoDTO> contactos) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" style=\"width:100%;border-collapse:collapse;\"><tr>")
                .append("<th ").append(TH).append(">Item</th>")
                .append("<th ").append(TH).append(">Nombres</th>")
                .append("<th ").append(TH).append(">Apellidos</th>")
                .append("<th ").append(TH).append(">Correo</th>")
                .append("<th ").append(TH).append(">Celular</th></tr>");
        int i = 1;
        for (ContactoTalentoDTO c : contactos != null ? contactos : List.<ContactoTalentoDTO>of()) {
            sb.append("<tr>")
                    .append("<td ").append(TD).append(">").append(i++).append("</td>")
                    .append("<td ").append(TD).append(">").append(esc(c.getNombres())).append("</td>")
                    .append("<td ").append(TD).append(">").append(esc(c.getApellidos())).append("</td>")
                    .append("<td ").append(TD).append(">").append(esc(c.getCorreo())).append("</td>")
                    .append("<td ").append(TD).append(">").append(esc(c.getCelular())).append("</td>")
                    .append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    /**
     * Cuerpo HTML de los correos de solicitud (creación de usuario / equipo): el
     * texto de siempre más la tabla de contacto de los talentos.
     */
    public static String cuerpoSolicitudHtml(String accion, List<ContactoTalentoDTO> contactos) {
        return "<div style=\"font-family:Arial,sans-serif;font-size:14px;\">"
                + "<p>Estimados,</p>"
                + "<p>Favor atender la solicitud de " + esc(accion) + ".</p>"
                + tablaContactoHtml(contactos)
                + "<p>Slds</p>"
                + "</div>";
    }

    private static String siNo(Integer valor) {
        return Integer.valueOf(1).equals(valor) ? "Sí" : "No";
    }

    private static String esc(String valor) {
        return valor == null ? "" : HtmlUtils.htmlEscape(valor);
    }
}
