package cl.copat.evalua.publico.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "enlace_encuesta")
public class EnlaceEncuesta implements Persistable<Long> {

  @Id
  @Column(name = "solicitud_id")
  private Long solicitudId;

  @Column(nullable = false, unique = true, length = 64)
  private String token;

  @Column(name = "creado_en", nullable = false)
  private LocalDateTime creadoEn;

  @Transient
  private boolean nuevo;

  protected EnlaceEncuesta() {}

  public EnlaceEncuesta(Long solicitudId, String token, LocalDateTime creadoEn) {
    this.solicitudId = solicitudId;
    this.token = token;
    this.creadoEn = creadoEn;
    this.nuevo = true;
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

  public String getToken() {
    return token;
  }

  public LocalDateTime getCreadoEn() {
    return creadoEn;
  }
}
