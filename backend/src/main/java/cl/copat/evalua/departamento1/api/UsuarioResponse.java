package cl.copat.evalua.departamento1.api;

public record UsuarioResponse(
    String email,
    String nombre,
    String cargo,
    String departamento,
    short departamentoId,
    boolean claveProvisoria) {}
