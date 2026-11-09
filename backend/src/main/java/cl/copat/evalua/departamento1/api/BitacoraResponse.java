package cl.copat.evalua.departamento1.api;

public record BitacoraResponse(
    long id,
    long solicitudId,
    String nombreEmpresa,
    String periodoInicio,
    String periodoFin,
    String fecha,
    String hora,
    String usuario,
    String accion) {}
