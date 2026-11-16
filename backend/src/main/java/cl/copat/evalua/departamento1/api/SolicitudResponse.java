package cl.copat.evalua.departamento1.api;

import java.util.List;

public record SolicitudResponse(
    long id,
    String nombreEmpresa,
    String rut,
    String correo,
    String telefono,
    String periodoInicio,
    String periodoFin,
    String plantilla,
    List<String> sedes,
    String descripcion,
    String estado,
    String creadoPor,
    String motivoRechazo,
    String rechazoPara,
    boolean volvioDeRechazo,
    String desempeno,
    String promedio,
    String seguimiento) {}
