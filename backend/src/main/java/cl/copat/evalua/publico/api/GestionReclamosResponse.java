package cl.copat.evalua.publico.api;

import java.util.List;

public record GestionReclamosResponse(
    String desde,
    String hasta,
    int total,
    List<Corte> porTipo,
    List<Corte> porArea,
    int reincidentes,
    String porcentajeReincidentes,
    String estadoReincidentes,
    int cerradosEnPlazo,
    String porcentajeCierre,
    String estadoCierre,
    String tiempoPromedio,
    String estadoTiempo,
    String principalesCausas,
    String cumplimientoPlazos,
    String responsable,
    String cargo,
    String aprobadoPor,
    String causasRecurrentes,
    String areasFrecuencia,
    String efectividad,
    String conclusiones,
    String comercialNombre,
    String comercialCargo,
    Long comercialUsuarioId,
    String comercialFecha,
    boolean comercialFirmaVirtual,
    String gerenciaNombre,
    String gerenciaCargo,
    Long gerenciaUsuarioId,
    String gerenciaFecha,
    boolean gerenciaFirmaVirtual,
    boolean comercialTieneFirma,
    boolean gerenciaTieneFirma) {

  public record Corte(String nombre, int cantidad, String porcentaje) {}
}
