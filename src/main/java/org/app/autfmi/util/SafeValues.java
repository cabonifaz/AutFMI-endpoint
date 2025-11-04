package org.app.autfmi.util;

public class SafeValues {

  /**
   * Maneja errores de forma segura para evitar NullPointerExceptions
   * 
   * @param value El objeto que puede ser nulo
   * @return Una cadena segura, vacía si el objeto es nulo
   */
  public static String safeString(Object value) {
    return value != null ? value.toString() : "";
  }
}
