package cl.copat.evalua.departamento2.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "solicitud_pdf")
public class SolicitudPdf implements Persistable<Long> {

  @Id
  @Column(name = "solicitud_id")
  private Long solicitudId;

  @Column(name = "nombre_archivo", nullable = false, length = 200)
  private String nombreArchivo;

  @Column(nullable = false)
  private byte[] contenido;

  @Transient
  private boolean nuevo;

  protected SolicitudPdf() {}

  public SolicitudPdf(Long solicitudId, String nombreArchivo, byte[] contenido) {
    this.solicitudId = solicitudId;
    this.nombreArchivo = nombreArchivo;
    this.contenido = contenido;
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

  public String getNombreArchivo() {
    return nombreArchivo;
  }

  public byte[] getContenido() {
    return contenido;
  }
}
