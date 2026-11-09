package cl.copat.evalua.departamento1.dominio;

import java.util.List;

public final class Catalogo {

  public static final short DEPARTAMENTO_1 = 1;
  public static final String ESTADO_INICIAL = "solicitud creada";
  public static final String ACCION_CREADA = "Solicitud creada";

  public static final List<String> ESTADOS = List.of(
      "solicitud creada",
      "solicitud enviada",
      "solicitud enviada a revision",
      "solicitud aprobada",
      "rechazada");

  public static final List<String> PLANTILLAS = List.of(
      "Guia evaluacion",
      "Guia evaluacion 2");

  private Catalogo() {}
}
