package org.app.autfmi.util;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilidades para validar y sanear direcciones de correo antes de entregarlas a
 * JavaMail. Una sola dirección malformada (por ejemplo una cadena vacía) hace
 * que el envío completo falle, por lo que conviene depurar las listas antes de
 * llamar a {@code helper.setCc(...)}.
 */
public final class EmailUtils {

    private EmailUtils() {
    }

    /**
     * Valida el formato de una dirección usando el parser RFC de JavaMail.
     *
     * @param email dirección a validar (puede ser nula)
     * @return {@code true} solo si tiene un formato de correo válido
     */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            InternetAddress address = new InternetAddress(trimmed, true); // strict
            address.validate();
            // InternetAddress acepta direcciones sin dominio ("usuario"); exigimos una arroba real.
            return trimmed.indexOf('@') > 0;
        } catch (AddressException e) {
            return false;
        }
    }

    /**
     * Limpia una lista de destinatarios: descarta nulos/vacíos y con formato
     * inválido, recorta espacios, elimina duplicados (sin distinguir mayúsculas)
     * y excluye la dirección indicada en {@code exclude} (normalmente el
     * destinatario principal / TO, para no repetirlo en CC).
     *
     * @param emails  lista original de correos (puede ser nula)
     * @param exclude dirección a excluir del resultado (puede ser nula)
     * @return lista depurada, nunca nula
     */
    public static List<String> sanitizeRecipients(Collection<String> emails, String exclude) {
        List<String> result = new ArrayList<>();
        if (emails == null || emails.isEmpty()) {
            return result;
        }

        String excludeNorm = exclude == null ? null : exclude.trim().toLowerCase();
        Set<String> seen = new LinkedHashSet<>();

        for (String email : emails) {
            if (!isValidEmail(email)) {
                continue;
            }
            String trimmed = email.trim();
            String norm = trimmed.toLowerCase();
            if (excludeNorm != null && norm.equals(excludeNorm)) {
                continue;
            }
            if (seen.add(norm)) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
