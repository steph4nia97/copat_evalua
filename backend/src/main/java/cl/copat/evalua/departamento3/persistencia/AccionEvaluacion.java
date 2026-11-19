package cl.copat.evalua.departamento3.persistencia;

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
import java.time.LocalDate;

@Entity
@Table(name = "accion_evaluacion")
public class AccionEvaluacion {

  public static final String EN_GESTION = "En gestión";
  public static final String CERRADO = "Cerrado";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false)
  private Solicitud solicitud;

  @Column(nullable = false, length = 400)
  private String descripcion;

  @Column(nullable = false, length = 80)
  private String responsable;

  @Column(nullable = false)
  private LocalDate plazo;

  @Column(nullable = false, length = 20)
  private String estado;

  protected AccionEvaluacion() {}

  public AccionEvaluacion(Solicitud solicitud, String descripcion, String responsable, LocalDate plazo, String estado) {
    this.solicitud = solicitud;
    this.descripcion = descripcion;
    this.responsable = responsable;
    this.plazo = plazo;
    this.estado = estado;
  }

  public void cambiarEstado(String estado) {
    this.estado = estado;
  }

  public Long getId() {
    return id;
  }

  public Solicitud getSolicitud() {
    return solicitud;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getResponsable() {
    return responsable;
  }

  public LocalDate getPlazo() {
    return plazo;
  }

  public String getEstado() {
    return estado;
  }
}
