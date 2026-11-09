package cl.copat.evalua.departamento1.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_solicitud")
public class EstadoSolicitud {

  @Id
  private Short id;

  @Column(nullable = false, length = 40)
  private String nombre;

  protected EstadoSolicitud() {}

  public Short getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }
}
