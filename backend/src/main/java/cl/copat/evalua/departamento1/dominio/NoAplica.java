package cl.copat.evalua.departamento1.dominio;

public final class NoAplica {

  public static final String TEXTO = "No aplica";

  private NoAplica() {}

  public static boolean es(String valor) {
    return valor != null && TEXTO.equalsIgnoreCase(valor.trim());
  }

  public static String correo(String valor) {
    String normalizado = Correo.normalizar(valor);
    if (normalizado.isEmpty() || es(normalizado)) {
      return TEXTO;
    }
    return normalizado;
  }

  public static boolean correoAceptado(String valor) {
    String normalizado = Correo.normalizar(valor);
    return normalizado.isEmpty() || es(normalizado) || Correo.esValido(normalizado);
  }

  public static String telefono(String valor) {
    String texto = valor == null ? "" : valor.trim();
    if (texto.isEmpty() || es(texto)) {
      return TEXTO;
    }
    return texto.length() > 20 ? texto.substring(0, 20) : texto;
  }
}
