package cl.copat.evalua.departamento1.dominio;

import java.util.regex.Pattern;

public final class NombreEmpresa {

  public static final int MAX = 100;
  private static final Pattern PATRON = Pattern.compile(
      "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]+( [A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]+)*$");

  private NombreEmpresa() {}

  public static String normalizar(String valor) {
    if (valor == null) {
      return "";
    }
    return valor.trim().replaceAll("\\s+", " ");
  }

  public static boolean esValido(String normalizado) {
    return !normalizado.isEmpty()
        && normalizado.length() <= MAX
        && PATRON.matcher(normalizado).matches();
  }
}
