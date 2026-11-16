package cl.copat.evalua.departamento1.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "solicitud_sede",
      joinColumns = @JoinColumn(name = "solicitud_id"),
      inverseJoinColumns = @JoinColumn(name = "sede_id"))
  @OrderBy("id ASC")
  private Set<Sede> sedes = new LinkedHashSet<>();

  @Column(name = "veces_rechazada", nullable = false)
  private int vecesRechazada;

  @Column(length = 40)
  private String seguimiento;

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

  public void cambiarEstado(EstadoSolicitud estado) {
    this.estado = estado;
  }

  public void asignarSedes(Collection<Sede> elegidas) {
    sedes.clear();
    sedes.addAll(elegidas);
  }

  public List<String> nombresSedes() {
    return sedes.stream().sorted(Comparator.comparing(Sede::getId)).map(Sede::getNombre).toList();
  }

  public int getVecesRechazada() {
    return vecesRechazada;
  }

  public void registrarRechazo() {
    this.vecesRechazada++;
  }

  public String getSeguimiento() {
    return seguimiento;
  }

  public void definirSeguimiento(String seguimiento) {
    this.seguimiento = seguimiento;
  }
}
