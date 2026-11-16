package cl.copat.evalua.departamento2.persistencia;

import cl.copat.evalua.departamento1.persistencia.Departamento;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
public class Notificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false)
  private Solicitud solicitud;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "departamento_id", nullable = false)
  private Departamento departamento;

  @Column(nullable = false, length = 200)
  private String mensaje;

  @Column(name = "creada_en", nullable = false)
  private LocalDateTime creadaEn;

  protected Notificacion() {}

  public Notificacion(Solicitud solicitud, Departamento departamento, String mensaje, LocalDateTime creadaEn) {
    this.solicitud = solicitud;
    this.departamento = departamento;
    this.mensaje = mensaje;
    this.creadaEn = creadaEn;
  }

  public Long getId() {
    return id;
  }

  public Solicitud getSolicitud() {
    return solicitud;
  }

  public Departamento getDepartamento() {
    return departamento;
  }

  public String getMensaje() {
    return mensaje;
  }

  public LocalDateTime getCreadaEn() {
    return creadaEn;
  }
}
