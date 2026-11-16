package cl.copat.evalua.shared.persistencia;

import cl.copat.evalua.departamento1.persistencia.Departamento;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.Usuario;
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
import java.time.LocalTime;

@Entity
@Table(name = "registro_actividad")
public class RegistroActividad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(nullable = false)
  private LocalTime hora;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "departamento_id", nullable = false)
  private Departamento departamento;

  @Column(nullable = false, length = 80)
  private String accion;

  @Column(nullable = false, length = 300)
  private String detalle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "solicitud_id")
  private Solicitud solicitud;

  protected RegistroActividad() {}

  public RegistroActividad(
      LocalDate fecha,
      LocalTime hora,
      Usuario usuario,
      Departamento departamento,
      String accion,
      String detalle,
      Solicitud solicitud) {
    this.fecha = fecha;
    this.hora = hora;
    this.usuario = usuario;
    this.departamento = departamento;
    this.accion = accion;
    this.detalle = detalle;
    this.solicitud = solicitud;
  }

  public Long getId() {
    return id;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public LocalTime getHora() {
    return hora;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Departamento getDepartamento() {
    return departamento;
  }

  public String getAccion() {
    return accion;
  }

  public String getDetalle() {
    return detalle;
  }
}
