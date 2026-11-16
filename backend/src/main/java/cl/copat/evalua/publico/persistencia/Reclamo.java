package cl.copat.evalua.publico.persistencia;

import cl.copat.evalua.departamento1.persistencia.Sede;
import cl.copat.evalua.publico.dominio.ReclamoCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reclamo")
public class Reclamo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false, length = 20)
  private String telefono;

  @Column(nullable = false, length = 200)
  private String obra;

  @Column(name = "fecha_servicio", nullable = false)
  private LocalDate fechaServicio;

  @Column(nullable = false, length = 150)
  private String empresa;

  @Column(nullable = false, length = 254)
  private String correo;

  @Column(nullable = false, length = 200)
  private String ubicacion;

  @Column(name = "guia_despacho", length = 40)
  private String guiaDespacho;

  @Column(nullable = false, length = 40)
  private String tipo;

  @Column(nullable = false, length = 1000)
  private String descripcion;

  @Column(name = "preferencia_contacto", nullable = false, length = 20)
  private String preferenciaContacto;

  @Column(length = 40)
  private String horario;

  @Column(length = 500)
  private String comentarios;

  @Column(name = "recibido_en", nullable = false)
  private LocalDateTime recibidoEn;

  @Column(nullable = false, length = 20)
  private String estado;

  @Column(name = "area_responsable", length = 80)
  private String areaResponsable;

  @Column(length = 80)
  private String responsable;

  @Column(name = "fecha_gestion")
  private LocalDate fechaGestion;

  @Column(length = 500)
  private String observaciones;

  @Column(name = "fecha_cierre")
  private LocalDate fechaCierre;

  @Column(name = "hora_cierre")
  private LocalTime horaCierre;

  @Column(length = 1000)
  private String solucion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sede_id")
  private Sede sede;

  protected Reclamo() {}

  public Reclamo(
      String nombre,
      String telefono,
      String obra,
      LocalDate fechaServicio,
      String empresa,
      String correo,
      String ubicacion,
      String guiaDespacho,
      String tipo,
      String descripcion,
      String preferenciaContacto,
      String horario,
      String comentarios,
      LocalDateTime recibidoEn) {
    this.nombre = nombre;
    this.telefono = telefono;
    this.obra = obra;
    this.fechaServicio = fechaServicio;
    this.empresa = empresa;
    this.correo = correo;
    this.ubicacion = ubicacion;
    this.guiaDespacho = guiaDespacho;
    this.tipo = tipo;
    this.descripcion = descripcion;
    this.preferenciaContacto = preferenciaContacto;
    this.horario = horario;
    this.comentarios = comentarios;
    this.recibidoEn = recibidoEn;
    this.estado = ReclamoCatalogo.ESTADO_RECIBIDO;
  }

  public void registrarSeguimiento(
      String estado,
      String areaResponsable,
      String responsable,
      LocalDate fechaGestion,
      String observaciones,
      LocalDate fechaCierre,
      LocalTime horaCierre,
      String solucion,
      Sede sede) {
    this.estado = estado;
    this.areaResponsable = areaResponsable;
    this.responsable = responsable;
    this.fechaGestion = fechaGestion;
    this.observaciones = observaciones;
    this.fechaCierre = fechaCierre;
    this.horaCierre = horaCierre;
    this.solucion = solucion;
    this.sede = sede;
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getTelefono() {
    return telefono;
  }

  public String getObra() {
    return obra;
  }

  public LocalDate getFechaServicio() {
    return fechaServicio;
  }

  public String getEmpresa() {
    return empresa;
  }

  public String getCorreo() {
    return correo;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public String getGuiaDespacho() {
    return guiaDespacho;
  }

  public String getTipo() {
    return tipo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getPreferenciaContacto() {
    return preferenciaContacto;
  }

  public String getHorario() {
    return horario;
  }

  public String getComentarios() {
    return comentarios;
  }

  public LocalDateTime getRecibidoEn() {
    return recibidoEn;
  }

  public String getEstado() {
    return estado;
  }

  public String getAreaResponsable() {
    return areaResponsable;
  }

  public String getResponsable() {
    return responsable;
  }

  public LocalDate getFechaGestion() {
    return fechaGestion;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public LocalDate getFechaCierre() {
    return fechaCierre;
  }

  public LocalTime getHoraCierre() {
    return horaCierre;
  }

  public String getSolucion() {
    return solucion;
  }

  public Sede getSede() {
    return sede;
  }
}
