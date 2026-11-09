package cl.copat.evalua.departamento1.persistencia;

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

@Entity
@Table(name = "solicitud")
public class Solicitud {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre_empresa", nullable = false, length = 100)
  private String nombreEmpresa;

  @Column(nullable = false, length = 12)
  private String rut;

  @Column(nullable = false, length = 254)
  private String correo;

  @Column(length = 11)
  private String telefono;

  @Column(name = "periodo_inicio", nullable = false)
  private LocalDate periodoInicio;

  @Column(name = "periodo_fin", nullable = false)
  private LocalDate periodoFin;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantilla_id", nullable = false)
  private Plantilla plantilla;

  @Column(length = 300)
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "estado_id", nullable = false)
  private EstadoSolicitud estado;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "departamento_id", nullable = false)
  private Departamento departamento;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "creado_por_id", nullable = false)
  private Usuario creadoPor;

  protected Solicitud() {}

  public Solicitud(
      String nombreEmpresa,
      String rut,
      String correo,
      String telefono,
      LocalDate periodoInicio,
      LocalDate periodoFin,
      Plantilla plantilla,
      String descripcion,
      EstadoSolicitud estado,
      Departamento departamento,
      Usuario creadoPor) {
    this.nombreEmpresa = nombreEmpresa;
    this.rut = rut;
    this.correo = correo;
    this.telefono = telefono;
    this.periodoInicio = periodoInicio;
    this.periodoFin = periodoFin;
    this.plantilla = plantilla;
    this.descripcion = descripcion;
    this.estado = estado;
    this.departamento = departamento;
    this.creadoPor = creadoPor;
  }

  public Long getId() {
    return id;
  }

  public String getNombreEmpresa() {
    return nombreEmpresa;
  }

  public String getRut() {
    return rut;
  }

  public String getCorreo() {
    return correo;
  }

  public String getTelefono() {
    return telefono;
  }

  public LocalDate getPeriodoInicio() {
    return periodoInicio;
  }

  public LocalDate getPeriodoFin() {
    return periodoFin;
  }

  public Plantilla getPlantilla() {
    return plantilla;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public EstadoSolicitud getEstado() {
    return estado;
  }

  public Departamento getDepartamento() {
    return departamento;
  }

  public Usuario getCreadoPor() {
    return creadoPor;
  }
}
