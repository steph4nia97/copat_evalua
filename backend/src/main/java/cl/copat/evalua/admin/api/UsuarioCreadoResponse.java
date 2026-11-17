package cl.copat.evalua.admin.api;

public record UsuarioCreadoResponse(
    long id,
    String email,
    String nombre,
    String cargo,
    String departamento,
    short departamentoId,
    boolean claveProvisoria,
    boolean activo) {}
