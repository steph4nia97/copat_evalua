package cl.copat.evalua.departamento3.persistencia;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "seguimiento_nota")
public class SeguimientoNota {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false)
  private Solicitud solicitud;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, length = 40)
  private String estado;

  @Column(nullable = false, length = 500)
  private String nota;

  @Column(name = "creada_en", nullable = false)
  private LocalDateTime creadaEn;

  protected SeguimientoNota() {}

  public SeguimientoNota(Solicitud solicitud, Usuario usuario, String estado, String nota, LocalDateTime creadaEn) {
    this.solicitud = solicitud;
    this.usuario = usuario;
    this.estado = estado;
    this.nota = nota;
    this.creadaEn = creadaEn;
  }

  public Long getId() {
    return id;
  }

  public Solicitud getSolicitud() {
    return solicitud;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getEstado() {
    return estado;
  }

  public String getNota() {
    return nota;
  }

  public LocalDateTime getCreadaEn() {
    return creadaEn;
  }
}
