package cl.copat.evalua.departamento1.api;

public record SolicitudResponse(
    long id,
    String nombreEmpresa,
    String rut,
    String correo,
    String telefono,
    String periodoInicio,
    String periodoFin,
    String plantilla,
    String descripcion,
    String estado,
    String creadoPor) {}
