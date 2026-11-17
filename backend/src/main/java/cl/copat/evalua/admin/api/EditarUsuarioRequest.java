package cl.copat.evalua.admin.api;

public record EditarUsuarioRequest(
    String correo,
    String nombre,
    String cargo,
    Short departamentoId,
    String clave) {}
