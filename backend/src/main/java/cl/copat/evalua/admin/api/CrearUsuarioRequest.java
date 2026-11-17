package cl.copat.evalua.admin.api;

public record CrearUsuarioRequest(String correo, String clave, String nombre, String cargo, Short departamentoId) {}
