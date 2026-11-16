package cl.copat.evalua.departamento3.persistencia;

import cl.copat.evalua.departamento1.persistencia.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "revision_departamento3")
public class RevisionDepartamento3 implements Persistable<Long> {

  @Id
  @Column(name = "solicitud_id")
  private Long solicitudId;

  @Column(nullable = false, length = 500)
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "actualizada_en", nullable = false)
  private LocalDateTime actualizadaEn;

  @Transient
  private boolean nuevo;

  protected RevisionDepartamento3() {}

  public RevisionDepartamento3(Long solicitudId, String descripcion, Usuario usuario, LocalDateTime actualizadaEn) {
    this.solicitudId = solicitudId;
    this.descripcion = descripcion;
    this.usuario = usuario;
    this.actualizadaEn = actualizadaEn;
    this.nuevo = true;
  }

  public void actualizar(String descripcion, Usuario usuario, LocalDateTime actualizadaEn) {
    this.descripcion = descripcion;
    this.usuario = usuario;
    this.actualizadaEn = actualizadaEn;
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
}
