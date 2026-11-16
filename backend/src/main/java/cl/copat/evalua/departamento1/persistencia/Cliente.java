package cl.copat.evalua.departamento1.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

  @Id
  @Column(length = 12)
  private String rut;

  @Column(name = "nombre_empresa", nullable = false, length = 150)
  private String nombreEmpresa;

  protected Cliente() {}

  public Cliente(String rut, String nombreEmpresa) {
    this.rut = rut;
    this.nombreEmpresa = nombreEmpresa;
  }

  public String getRut() {
    return rut;
  }

  public String getNombreEmpresa() {
    return nombreEmpresa;
  }

  public void renombrar(String nombreEmpresa) {
    this.nombreEmpresa = nombreEmpresa;
  }
}
