package cl.copat.evalua.publico.api;

public record EnlaceResponse(
    String token,
    boolean abierta,
    boolean correoEnviado,
    String destino,
    String aviso) {}
