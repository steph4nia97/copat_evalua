package cl.copat.evalua.departamento2.persistencia;

import cl.copat.evalua.departamento2.dominio.GuiaPdf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "solicitud_evaluacion")
public class SolicitudEvaluacion implements Persistable<Long> {

  @Id
  @Column(name = "solicitud_id")
  private Long solicitudId;

  @Column(name = "nombre_encuestado", nullable = false, length = 120)
  private String nombreEncuestado;

  @Column(name = "empresa_encuesta", nullable = false, length = 150)
  private String empresaEncuesta;

  @Column(nullable = false, length = 120)
  private String cargo;

  @Column(nullable = false, length = 200)
  private String obra;

  @Column(name = "estado_obra", nullable = false, length = 40)
  private String estadoObra;

  @Column(length = 1000)
  private String comentarios;

  @Column(name = "gestion_administrativa", nullable = false)
  private short gestionAdministrativa;

  @Column(name = "tiempos_entrega", nullable = false)
  private short tiemposEntrega;

  @Column(name = "estado_hormigon", nullable = false)
  private short estadoHormigon;

  @Column
  private Short flexibilidad;

  @Column(name = "atencion_comercial", nullable = false)
  private short atencionComercial;

  @Column(name = "respuesta_reclamos")
  private Short respuestaReclamos;

  @Column(name = "entrega_informacion", nullable = false)
  private short entregaInformacion;

  @Column(name = "competitividad_precio", nullable = false)
  private short competitividadPrecio;

  @Column(name = "capacitacion_tecnica", nullable = false)
  private short capacitacionTecnica;

  @Column(name = "percepcion_general", nullable = false)
  private short percepcionGeneral;

  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal promedio;

  @Column(nullable = false, length = 40)
  private String desempeno;

  @Column(name = "recibida_en", nullable = false)
  private LocalDateTime recibidaEn;

  @Transient
  private boolean nuevo;

  protected SolicitudEvaluacion() {}

  public SolicitudEvaluacion(Long solicitudId, GuiaPdf.Lectura lectura, LocalDateTime recibidaEn) {
    this.solicitudId = solicitudId;
    this.nombreEncuestado = lectura.nombre();
    this.empresaEncuesta = lectura.empresa();
    this.cargo = lectura.cargo();
    this.obra = lectura.obra();
    this.estadoObra = lectura.estadoObra();
    this.comentarios = lectura.comentarios();
    this.gestionAdministrativa = nota(lectura, "gestion_administrativa");
    this.tiemposEntrega = nota(lectura, "tiempos_entrega");
    this.estadoHormigon = nota(lectura, "estado_hormigon");
    this.flexibilidad = notaOpcional(lectura, "flexibilidad");
    this.atencionComercial = nota(lectura, "atencion_comercial");
    this.respuestaReclamos = notaOpcional(lectura, "respuesta_reclamos");
    this.entregaInformacion = nota(lectura, "entrega_informacion");
    this.competitividadPrecio = nota(lectura, "competitividad_precio");
    this.capacitacionTecnica = nota(lectura, "capacitacion_tecnica");
    this.percepcionGeneral = nota(lectura, "percepcion_general");
    this.promedio = lectura.promedio();
    this.desempeno = lectura.desempeno();
    this.recibidaEn = recibidaEn;
    this.nuevo = true;
  }

  public void reemplazar(GuiaPdf.Lectura lectura, LocalDateTime recibidaEn) {
    this.nombreEncuestado = lectura.nombre();
    this.empresaEncuesta = lectura.empresa();
    this.cargo = lectura.cargo();
    this.obra = lectura.obra();
    this.estadoObra = lectura.estadoObra();
    this.comentarios = lectura.comentarios();
    this.gestionAdministrativa = nota(lectura, "gestion_administrativa");
    this.tiemposEntrega = nota(lectura, "tiempos_entrega");
    this.estadoHormigon = nota(lectura, "estado_hormigon");
    this.flexibilidad = notaOpcional(lectura, "flexibilidad");
    this.atencionComercial = nota(lectura, "atencion_comercial");
    this.respuestaReclamos = notaOpcional(lectura, "respuesta_reclamos");
    this.entregaInformacion = nota(lectura, "entrega_informacion");
    this.competitividadPrecio = nota(lectura, "competitividad_precio");
    this.capacitacionTecnica = nota(lectura, "capacitacion_tecnica");
    this.percepcionGeneral = nota(lectura, "percepcion_general");
    this.promedio = lectura.promedio();
    this.desempeno = lectura.desempeno();
    this.recibidaEn = recibidaEn;
    this.nuevo = false;
  }

  private static short nota(GuiaPdf.Lectura lectura, String clave) {
    Integer valor = lectura.notas().get(clave);
    if (valor == null) {
      throw new IllegalStateException("Falta la nota " + clave);
    }
    return valor.shortValue();
  }

  private static Short notaOpcional(GuiaPdf.Lectura lectura, String clave) {
    Integer valor = lectura.notas().get(clave);
    return valor == null ? null : valor.shortValue();
  }

  @Override
  public Long getId() {
    return solicitudId;
  }

  @Override
  public boolean isNew() {
    return nuevo;
  }

  @PostLoad
  @PostPersist
  void marcarPersistida() {
    this.nuevo = false;
  }

  public Integer nota(String clave) {
    return switch (clave) {
      case "gestion_administrativa" -> entero(gestionAdministrativa);
      case "tiempos_entrega" -> entero(tiemposEntrega);
      case "estado_hormigon" -> entero(estadoHormigon);
      case "flexibilidad" -> entero(flexibilidad);
      case "atencion_comercial" -> entero(atencionComercial);
      case "respuesta_reclamos" -> entero(respuestaReclamos);
      case "entrega_informacion" -> entero(entregaInformacion);
      case "competitividad_precio" -> entero(competitividadPrecio);
      case "capacitacion_tecnica" -> entero(capacitacionTecnica);
      case "percepcion_general" -> entero(percepcionGeneral);
      default -> throw new IllegalArgumentException(clave);
    };
  }

  private static Integer entero(short valor) {
    return (int) valor;
  }

  private static Integer entero(Short valor) {
    return valor == null ? null : valor.intValue();
  }

  public String getNombreEncuestado() {
    return nombreEncuestado;
  }

  public String getEmpresaEncuesta() {
    return empresaEncuesta;
  }

  public String getCargo() {
    return cargo;
  }

  public String getObra() {
    return obra;
  }

  public String getEstadoObra() {
    return estadoObra;
  }

  public String getComentarios() {
    return comentarios;
  }

  public BigDecimal getPromedio() {
    return promedio;
  }

  public String getDesempeno() {
    return desempeno;
  }

  public LocalDateTime getRecibidaEn() {
    return recibidaEn;
  }
}
