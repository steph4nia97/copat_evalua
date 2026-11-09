package cl.copat.evalua.departamento1.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plantilla")
public class Plantilla {

  @Id
  private Short id;

  @Column(nullable = false, length = 40)
  private String nombre;

  protected Plantilla() {}

  public Short getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }
}
