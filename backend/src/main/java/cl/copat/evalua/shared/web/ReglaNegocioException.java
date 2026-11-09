package cl.copat.evalua.shared.web;

import java.util.Map;

public class ReglaNegocioException extends RuntimeException {

  private final Map<String, String> campos;

  public ReglaNegocioException(String mensaje) {
    this(mensaje, Map.of());
  }

  public ReglaNegocioException(String mensaje, Map<String, String> campos) {
    super(mensaje);
    this.campos = campos == null ? Map.of() : Map.copyOf(campos);
  }

  public Map<String, String> getCampos() {
    return campos;
  }
}
