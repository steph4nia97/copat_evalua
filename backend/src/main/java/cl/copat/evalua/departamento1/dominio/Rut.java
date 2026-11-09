package cl.copat.evalua.departamento1.dominio;

import java.util.Locale;

public final class Rut {

  private Rut() {}

  public static boolean esValido(String valor) {
    String limpio = limpiar(valor);
    if (limpio == null || !limpio.matches("\\d{7,8}[0-9K]")) {
      return false;
    }
    String cuerpo = limpio.substring(0, limpio.length() - 1);
    char dv = limpio.charAt(limpio.length() - 1);
    return dv == calcularDv(cuerpo);
  }

  public static String normalizar(String valor) {
    String limpio = limpiar(valor);
    if (limpio == null || limpio.length() < 2) {
      return "";
    }
    return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
  }

  public static String formatear(String normalizado) {
    int guion = normalizado.lastIndexOf('-');
    String cuerpo = normalizado.substring(0, guion);
    String dv = normalizado.substring(guion + 1);
    StringBuilder invertido = new StringBuilder();
    int grupo = 0;
    for (int i = cuerpo.length() - 1; i >= 0; i--) {
      if (grupo == 3) {
        invertido.append('.');
        grupo = 0;
      }
      invertido.append(cuerpo.charAt(i));
      grupo++;
    }
    return invertido.reverse() + "-" + dv;
  }

  public static char calcularDv(String cuerpo) {
    int suma = 0;
    int factor = 2;
    for (int i = cuerpo.length() - 1; i >= 0; i--) {
      suma += (cuerpo.charAt(i) - '0') * factor;
      factor = factor == 7 ? 2 : factor + 1;
    }
    int resto = 11 - (suma % 11);
    if (resto == 11) {
      return '0';
    }
    if (resto == 10) {
      return 'K';
    }
    return (char) ('0' + resto);
  }

  private static String limpiar(String valor) {
    if (valor == null) {
      return null;
    }
    String limpio = valor.replace(".", "").replace("-", "").replace(" ", "").trim();
    if (limpio.isEmpty()) {
      return null;
    }
    return limpio.toUpperCase(Locale.ROOT);
  }
}
