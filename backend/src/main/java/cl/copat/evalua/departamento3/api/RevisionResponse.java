package cl.copat.evalua.departamento3.api;

import java.math.BigDecimal;
import java.util.List;

public record RevisionResponse(
    long id,
    String nombreEmpresa,
    String rut,
    String correo,
    String telefono,
    String periodoInicio,
    String periodoFin,
    String plantilla,
    List<String> sedes,
    String descripcionSolicitud,
    String creadoPor,
    List<MovimientoResponse> movimientos,
    String encuestado,
    String empresaEncuesta,
    String cargo,
    String obra,
    String estadoObra,
    String comentarios,
    List<NotaResponse> notas,
    BigDecimal promedio,
    String desempeno,
    String escala,
    String descripcion,
    boolean tieneEvidencia,
    boolean tienePdf,
    boolean reenviada,
    String seguimiento,
    List<NotaSeguimientoResponse> notasSeguimiento) {

  public record MovimientoResponse(String accion, String trabajador, String fecha, String hora) {}

  public record NotaResponse(String etiqueta, Integer valor) {}

  public record NotaSeguimientoResponse(
      long id,
      String nota,
      String estado,
      String usuario,
      String fecha,
      String hora) {}
}
