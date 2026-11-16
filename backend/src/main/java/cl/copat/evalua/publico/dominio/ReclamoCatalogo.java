package cl.copat.evalua.publico.dominio;

import java.util.List;

public final class ReclamoCatalogo {

  public static final List<String> TIPOS = List.of(
      "Calidad del hormigón",
      "Retraso en la entrega",
      "Atención del personal",
      "Facturación / documentación",
      "Otro");

  public static final List<String> PREFERENCIAS = List.of("Teléfono", "Mail");

  public static final List<String> HORARIOS = List.of(
      "Mañana, 08:00 a 12:00",
      "Tarde, 12:00 a 17:00",
      "Cualquier horario");

  public static final String ESTADO_RECIBIDO = "Recibido";
  public static final String ESTADO_GESTION = "En gestión";
  public static final String ESTADO_CERRADO = "Cerrado";

  public static final List<String> ESTADOS = List.of(ESTADO_RECIBIDO, ESTADO_GESTION, ESTADO_CERRADO);

  private ReclamoCatalogo() {}
}
