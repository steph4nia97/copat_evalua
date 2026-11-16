package cl.copat.evalua.departamento1.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contacto")
public class Contacto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 12)
  private String rut;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false, length = 150)
  private String empresa;

  @Column(nullable = false, length = 254)
  private String correo;

  @Column(length = 20)
  private String telefono;

  protected Contacto() {}

  public Contacto(String rut, String nombre, String empresa, String correo, String telefono) {
    this.rut = rut;
    this.nombre = nombre;
    this.empresa = empresa;
    this.correo = correo;
    this.telefono = telefono;
  }

  public Long getId() {
    return id;
  }

  public String getRut() {
    return rut;
  }

  public String getNombre() {
    return nombre;
  }

  public String getEmpresa() {
    return empresa;
  }

  public String getCorreo() {
    return correo;
  }

  public String getTelefono() {
    return telefono;
  }

  public void actualizar(String nombre, String empresa, String telefono) {
    this.nombre = nombre;
    this.empresa = empresa;
    this.telefono = telefono;
  }
}
