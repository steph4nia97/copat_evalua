package cl.copat.evalua.departamento1.api;

public record CruceResponse(
    String rut,
    String empresa,
    String nombreContacto,
    String empresaContacto,
    String correo,
    String telefono) {}
