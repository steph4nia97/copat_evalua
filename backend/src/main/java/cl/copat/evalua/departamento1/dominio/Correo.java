package cl.copat.evalua.departamento1.dominio;

import java.util.regex.Pattern;

public final class Correo {

  public static final int MAX = 254;
  private static final Pattern PATRON = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  private Correo() {}

  public static String normalizar(String valor) {
    if (valor == null) {
      return "";
    }
    return valor.trim();
  }

  public static boolean esValido(String normalizado) {
    return !normalizado.isEmpty()
        && normalizado.length() <= MAX
        && PATRON.matcher(normalizado).matches();
  }
}
