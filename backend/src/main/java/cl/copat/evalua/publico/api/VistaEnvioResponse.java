package cl.copat.evalua.publico.api;

import java.util.List;

public record VistaEnvioResponse(
    String desde,
    String asunto,
    List<DestinatarioEnvio> destinatarios) {

  public record DestinatarioEnvio(String nombre, String correo, String telefono, String mensaje) {}
}
