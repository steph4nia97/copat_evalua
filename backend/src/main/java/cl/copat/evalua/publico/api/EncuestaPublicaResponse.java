package cl.copat.evalua.publico.api;

import java.util.List;
import java.util.Map;

public record EncuestaPublicaResponse(
    String empresa,
    String correo,
    String telefono,
    String periodoInicio,
    String periodoFin,
    List<String> sedes,
    boolean abierta,
    String motivoRechazo,
    List<PreguntaPublica> preguntas,
    RespuestaGuardada respuesta) {

  public record PreguntaPublica(String clave, String etiqueta) {}

  public record RespuestaGuardada(
      String nombre,
      String cargo,
      String obra,
      String estadoObra,
      String comentarios,
      Map<String, Integer> notas) {}
}
