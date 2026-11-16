package cl.copat.evalua.publico.api;

public record SeguimientoRequest(
    String estado,
    String area,
    String responsable,
    String fechaGestion,
    String observaciones,
    String fechaCierre,
    String horaCierre,
    String solucion,
    String sede) {}
