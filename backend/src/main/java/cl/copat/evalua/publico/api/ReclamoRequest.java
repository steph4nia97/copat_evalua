package cl.copat.evalua.publico.api;

public record ReclamoRequest(
    String nombre,
    String telefono,
    String obra,
    String fechaServicio,
    String empresa,
    String correo,
    String ubicacion,
    String guiaDespacho,
    String tipo,
    String descripcion,
    String preferenciaContacto,
    String horario,
    String comentarios) {}
