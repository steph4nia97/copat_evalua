package cl.copat.evalua.departamento1.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 80)
  private String nombre;

  @Column(name = "clave_provisoria", nullable = false)
  private boolean claveProvisoria;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "departamento_id", nullable = false)
  private Departamento departamento;

  protected Usuario() {}

  public Usuario(String email, String passwordHash, String nombre, Departamento departamento, boolean claveProvisoria) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.nombre = nombre;
    this.departamento = departamento;
    this.claveProvisoria = claveProvisoria;
  }

  public void renombrar(String nombre) {
    this.nombre = nombre;
  }

  public void cambiarClave(String passwordHash) {
    this.passwordHash = passwordHash;
    this.claveProvisoria = false;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getNombre() {
    return nombre;
  }

  public boolean isClaveProvisoria() {
    return claveProvisoria;
  }

  public Departamento getDepartamento() {
    return departamento;
  }
}
