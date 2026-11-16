package cl.copat.evalua.admin.api;

public record RegistroResponse(
    long id,
    String fecha,
    String hora,
    String trabajador,
    String departamento,
    String accion,
    String detalle) {}
