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
import java.time.LocalTime;

@Entity
@Table(name = "solicitud_log")
public class SolicitudLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false)
  private Solicitud solicitud;

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(nullable = false)
  private LocalTime hora;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, length = 80)
  private String accion;

  protected SolicitudLog() {}

  public SolicitudLog(Solicitud solicitud, LocalDate fecha, LocalTime hora, Usuario usuario, String accion) {
    this.solicitud = solicitud;
    this.fecha = fecha;
    this.hora = hora;
    this.usuario = usuario;
    this.accion = accion;
  }

  public Long getId() {
    return id;
  }

  public Solicitud getSolicitud() {
    return solicitud;
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

  public String getAccion() {
    return accion;
  }
}
