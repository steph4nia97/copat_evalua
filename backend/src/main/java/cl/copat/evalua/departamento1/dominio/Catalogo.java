package cl.copat.evalua.departamento1.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class Catalogo {

  public static final short DEPARTAMENTO_1 = 1;
  public static final short DEPARTAMENTO_2 = 2;
  public static final short DEPARTAMENTO_3 = 3;
  public static final short DEPARTAMENTO_ADMIN = 4;
  public static final String ESTADO_INICIAL = "solicitud ingresada";
  public static final String ESTADO_ENVIADA_CLIENTE = "solicitud enviada a cliente";
  public static final String ESTADO_EN_REVISION = "solicitud en revision";
  public static final String ACCION_CREADA = "Solicitud creada";
  public static final String ACCION_ENVIADA_CLIENTE = "Solicitud enviada a cliente";
  public static final String ACCION_EN_REVISION = "Solicitud enviada a revision";
  public static final String ACCION_REVISION_REGISTRADA = "Revision registrada";
  public static final String ACCION_RECHAZADA = "Solicitud rechazada";
  public static final String ACCION_REENVIADA = "Solicitud reenviada a revision";
  public static final String ACCION_USUARIO_CREADO = "Usuario creado";
  public static final String ACCION_NOMBRE = "Nombre actualizado";
  public static final String ACCION_CLAVE = "Contrasena actualizada";
  public static final String ACCION_CARGA_CLIENTES = "Datos de clientes cargados";
  public static final String ACCION_CARGA_CONTACTOS = "Contactos de clientes cargados";
  public static final String SEGUIMIENTO_RECIBIDA = "Recibida";
  public static final String SEGUIMIENTO_ANALISIS = "En análisis";
  public static final String SEGUIMIENTO_GESTION = "En gestión";
  public static final String SEGUIMIENTO_CERRADA = "Cerrada";
  public static final String VISTA_PROCESAMIENTO = "En procesamiento";
  public static final String VISTA_CERRADO = "Cerrado";
  public static final String PREFIJO_EVALUACION = "Evaluación: ";
  public static final String PREFIJO_SEGUIMIENTO = "Seguimiento: ";
  public static final String ESTADO_RECHAZADA = "solicitud rechazada";
  public static final String GUIA_1 = "Guia evaluacion";
  public static final String GUIA_2 = "Guia evaluacion 2";

  public static final List<String> ESTADOS = List.of(
      ESTADO_INICIAL,
      ESTADO_ENVIADA_CLIENTE,
      ESTADO_EN_REVISION,
      "solicitud aprobada",
      "solicitud rechazada");

  public static final List<String> PLANTILLAS = List.of(GUIA_1, GUIA_2);

  public static final List<String> SEGUIMIENTO = List.of(
      SEGUIMIENTO_RECIBIDA,
      SEGUIMIENTO_ANALISIS,
      SEGUIMIENTO_GESTION,
      SEGUIMIENTO_CERRADA);

  public static final List<String> SEDES = List.of(
      "Antofagasta",
      "Mejillones",
      "Calama",
      "María Elena",
      "Tocopilla",
      "Taltal");

  private Catalogo() {}

  public static boolean operaSolicitudes(short id) {
    return id == DEPARTAMENTO_1 || id == DEPARTAMENTO_2;
  }

  public static boolean seguimientoValido(String estado) {
    return estado != null && SEGUIMIENTO.contains(estado);
  }

  public static String vistaOperacion(String seguimiento) {
    if (seguimiento == null || seguimiento.isBlank()) {
      return null;
    }
    return SEGUIMIENTO_CERRADA.equals(seguimiento) ? VISTA_CERRADO : VISTA_PROCESAMIENTO;
  }

  public static String evaluacion(String desempeno, BigDecimal promedio) {
    String numero = promedio.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    return PREFIJO_EVALUACION + desempeno + " · " + numero;
  }

  public static String seguimiento(String estado) {
    return PREFIJO_SEGUIMIENTO + estado;
  }

  public static String etiquetaBitacora(String accion, boolean operacion) {
    if (ACCION_ENVIADA_CLIENTE.equals(accion)) {
      return "Envío de solicitud";
    }
    if (ACCION_EN_REVISION.equals(accion)) {
      return "Recepción de solicitud";
    }
    if (accion.startsWith(PREFIJO_EVALUACION) || ACCION_CARGA_CLIENTES.equals(accion) || ACCION_CARGA_CONTACTOS.equals(accion)) {
      return accion;
    }
    if (accion.startsWith(PREFIJO_SEGUIMIENTO)) {
      if (!operacion) {
        return accion;
      }
      String estado = accion.substring(PREFIJO_SEGUIMIENTO.length());
      return seguimiento(SEGUIMIENTO_CERRADA.equals(estado) ? VISTA_CERRADO : VISTA_PROCESAMIENTO);
    }
    return null;
  }

  public static String promedio(BigDecimal promedio) {
    if (promedio == null) {
      return null;
    }
    return promedio.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
  }

  public static String nombreDepartamento(short id) {
    return switch (id) {
      case DEPARTAMENTO_1 -> "Departamento 1";
      case DEPARTAMENTO_2 -> "Departamento 2";
      case DEPARTAMENTO_3 -> "Departamento 3";
      case DEPARTAMENTO_ADMIN -> "Administración";
      default -> "Departamento";
    };
  }
}
