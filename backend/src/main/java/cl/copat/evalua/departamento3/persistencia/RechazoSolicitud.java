package cl.copat.evalua.departamento3.persistencia;

import cl.copat.evalua.departamento1.persistencia.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "rechazo_solicitud")
public class RechazoSolicitud implements Persistable<Long> {

  @Id
  @Column(name = "solicitud_id")
  private Long solicitudId;

  @Column(nullable = false, length = 500)
  private String descripcion;

  @ManyToOne(optional = false)
  @JoinColumn(name = "rechazado_por_id", nullable = false)
  private Usuario rechazadoPor;

  @ManyToOne(optional = false)
  @JoinColumn(name = "destinatario_id", nullable = false)
  private Usuario destinatario;

  @Column(name = "rechazada_en", nullable = false)
  private LocalDateTime rechazadaEn;

  @Transient
  private boolean nuevo;

  protected RechazoSolicitud() {}

  public RechazoSolicitud(
      Long solicitudId,
      String descripcion,
      Usuario rechazadoPor,
      Usuario destinatario,
      LocalDateTime rechazadaEn) {
    this.solicitudId = solicitudId;
    this.descripcion = descripcion;
    this.rechazadoPor = rechazadoPor;
    this.destinatario = destinatario;
    this.rechazadaEn = rechazadaEn;
    this.nuevo = true;
  }

  public void actualizar(String descripcion, Usuario rechazadoPor, Usuario destinatario, LocalDateTime rechazadaEn) {
    this.descripcion = descripcion;
    this.rechazadoPor = rechazadoPor;
    this.destinatario = destinatario;
    this.rechazadaEn = rechazadaEn;
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

  public String getDescripcion() {
    return descripcion;
  }

  public Usuario getDestinatario() {
    return destinatario;
  }
}
