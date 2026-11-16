package cl.copat.evalua.publico.api;

import java.util.Map;

public record RespuestaEncuestaRequest(
    String nombre,
    String cargo,
    String obra,
    String estadoObra,
    String comentarios,
    Map<String, Integer> notas) {}
