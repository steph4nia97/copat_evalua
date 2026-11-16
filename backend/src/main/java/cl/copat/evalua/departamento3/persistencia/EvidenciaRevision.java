package cl.copat.evalua.departamento3.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "evidencia_revision")
public class EvidenciaRevision implements Persistable<Long> {

  @Id
  @Column(name = "solicitud_id")
  private Long solicitudId;

  @Column(name = "nombre_archivo", nullable = false, length = 200)
  private String nombreArchivo;

  @Column(name = "media_type", nullable = false, length = 100)
  private String mediaType;

  @Column(nullable = false)
  private byte[] contenido;

  @Transient
  private boolean nuevo;

  protected EvidenciaRevision() {}

  public EvidenciaRevision(Long solicitudId, String nombreArchivo, String mediaType, byte[] contenido) {
    this.solicitudId = solicitudId;
    this.nombreArchivo = nombreArchivo;
    this.mediaType = mediaType;
    this.contenido = contenido;
    this.nuevo = true;
  }

  public void reemplazar(String nombreArchivo, String mediaType, byte[] contenido) {
    this.nombreArchivo = nombreArchivo;
    this.mediaType = mediaType;
    this.contenido = contenido;
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

  public String getNombreArchivo() {
    return nombreArchivo;
  }

  public String getMediaType() {
    return mediaType;
  }

  public byte[] getContenido() {
    return contenido;
  }
}
