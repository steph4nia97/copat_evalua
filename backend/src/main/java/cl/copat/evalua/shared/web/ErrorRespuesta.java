package cl.copat.evalua.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

public record ErrorRespuesta(
    String mensaje,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> campos) {

  public ErrorRespuesta(String mensaje) {
    this(mensaje, Map.of());
  }
}
