package cl.copat.evalua.publico.api;

public record GestionReclamosRequest(
    String desde,
    String hasta,
    String responsable,
    String cargo,
    String aprobadoPor,
    String causasRecurrentes,
    String areasFrecuencia,
    String efectividad,
    String conclusiones,
    Long comercialUsuarioId,
    String comercialFecha,
    boolean comercialFirmaVirtual,
    boolean comercialQuitarFirma,
    Long gerenciaUsuarioId,
    String gerenciaFecha,
    boolean gerenciaFirmaVirtual,
    boolean gerenciaQuitarFirma) {}
