package cl.copat.evalua.departamento1.dominio;

import java.util.regex.Pattern;

public final class TelefonoChile {

  private static final Pattern PATRON = Pattern.compile("^(?:56)?[2-9]\\d{8}$");

  private TelefonoChile() {}

  public static boolean esVacio(String valor) {
    return valor == null || valor.isBlank();
  }

  public static boolean esValido(String valor) {
    return esVacio(valor) || PATRON.matcher(valor.trim()).matches();
  }

  public static String normalizar(String valor) {
    if (esVacio(valor)) {
      return null;
    }
    return valor.trim();
  }
}
