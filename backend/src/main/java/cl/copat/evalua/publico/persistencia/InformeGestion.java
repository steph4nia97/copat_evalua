package cl.copat.evalua.publico.persistencia;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
    name = "informe_gestion_reclamo",
    uniqueConstraints = @UniqueConstraint(name = "uq_informe_gestion_periodo", columnNames = {"periodo_inicio", "periodo_fin"}))
public class InformeGestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "periodo_inicio", nullable = false)
  private LocalDate periodoInicio;

  @Column(name = "periodo_fin", nullable = false)
  private LocalDate periodoFin;

  @Column(length = 80)
  private String responsable;

  @Column(length = 80)
  private String cargo;

  @Column(name = "aprobado_por", length = 80)
  private String aprobadoPor;

  @Column(name = "causas_recurrentes", length = 1000)
  private String causasRecurrentes;

  @Column(name = "areas_frecuencia", length = 1000)
  private String areasFrecuencia;

  @Column(length = 1000)
  private String efectividad;

  @Column(length = 2000)
  private String conclusiones;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comercial_usuario_id")
  private Usuario comercial;

  @Column(name = "comercial_nombre", length = 80)
  private String comercialNombre;

  @Column(name = "comercial_cargo", length = 80)
  private String comercialCargo;

  @Column(name = "comercial_fecha")
  private LocalDate comercialFecha;

  @Column(name = "comercial_firma_virtual", nullable = false)
  private boolean comercialFirmaVirtual;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gerencia_usuario_id")
  private Usuario gerencia;

  @Column(name = "gerencia_nombre", length = 80)
  private String gerenciaNombre;

  @Column(name = "gerencia_cargo", length = 80)
  private String gerenciaCargo;

  @Column(name = "gerencia_fecha")
  private LocalDate gerenciaFecha;

  @Column(name = "gerencia_firma_virtual", nullable = false)
  private boolean gerenciaFirmaVirtual;

  @Column(name = "comercial_firma")
  private byte[] comercialFirma;

  @Column(name = "comercial_firma_tipo", length = 40)
  private String comercialFirmaTipo;

  @Column(name = "gerencia_firma")
  private byte[] gerenciaFirma;

  @Column(name = "gerencia_firma_tipo", length = 40)
  private String gerenciaFirmaTipo;

  protected InformeGestion() {}

  public InformeGestion(LocalDate periodoInicio, LocalDate periodoFin) {
    this.periodoInicio = periodoInicio;
    this.periodoFin = periodoFin;
  }

  public void completar(
      String responsable,
      String cargo,
      String aprobadoPor,
      String causasRecurrentes,
      String areasFrecuencia,
      String efectividad,
      String conclusiones,
      Usuario comercial,
      String comercialNombre,
      String comercialCargo,
      LocalDate comercialFecha,
      boolean comercialFirmaVirtual,
      Usuario gerencia,
      String gerenciaNombre,
      String gerenciaCargo,
      LocalDate gerenciaFecha,
      boolean gerenciaFirmaVirtual) {
    this.responsable = responsable;
    this.cargo = cargo;
    this.aprobadoPor = aprobadoPor;
    this.causasRecurrentes = causasRecurrentes;
    this.areasFrecuencia = areasFrecuencia;
    this.efectividad = efectividad;
    this.conclusiones = conclusiones;
    this.comercial = comercial;
    this.comercialNombre = comercialNombre;
    this.comercialCargo = comercialCargo;
    this.comercialFecha = comercialFecha;
    this.comercialFirmaVirtual = comercialFirmaVirtual;
    this.gerencia = gerencia;
    this.gerenciaNombre = gerenciaNombre;
    this.gerenciaCargo = gerenciaCargo;
    this.gerenciaFecha = gerenciaFecha;
    this.gerenciaFirmaVirtual = gerenciaFirmaVirtual;
  }

  public String getResponsable() {
    return responsable;
  }

  public String getCargo() {
    return cargo;
  }

  public String getAprobadoPor() {
    return aprobadoPor;
  }

  public String getCausasRecurrentes() {
    return causasRecurrentes;
  }

  public String getAreasFrecuencia() {
    return areasFrecuencia;
  }

  public String getEfectividad() {
    return efectividad;
  }

  public String getConclusiones() {
    return conclusiones;
  }

  public Usuario getComercial() {
    return comercial;
  }

  public String getComercialNombre() {
    return comercialNombre;
  }

  public String getComercialCargo() {
    return comercialCargo;
  }

  public LocalDate getComercialFecha() {
    return comercialFecha;
  }

  public boolean isComercialFirmaVirtual() {
    return comercialFirmaVirtual;
  }

  public Usuario getGerencia() {
    return gerencia;
  }

  public String getGerenciaNombre() {
    return gerenciaNombre;
  }

  public String getGerenciaCargo() {
    return gerenciaCargo;
  }

  public LocalDate getGerenciaFecha() {
    return gerenciaFecha;
  }

  public boolean isGerenciaFirmaVirtual() {
    return gerenciaFirmaVirtual;
  }

  public byte[] getComercialFirma() {
    return comercialFirma;
  }

  public String getComercialFirmaTipo() {
    return comercialFirmaTipo;
  }

  public byte[] getGerenciaFirma() {
    return gerenciaFirma;
  }

  public String getGerenciaFirmaTipo() {
    return gerenciaFirmaTipo;
  }

  public boolean tieneFirmaComercial() {
    return comercialFirma != null && comercialFirma.length > 0;
  }

  public boolean tieneFirmaGerencia() {
    return gerenciaFirma != null && gerenciaFirma.length > 0;
  }

  public void asignarFirmaComercial(byte[] contenido, String tipo) {
    this.comercialFirma = contenido;
    this.comercialFirmaTipo = tipo;
  }

  public void asignarFirmaGerencia(byte[] contenido, String tipo) {
    this.gerenciaFirma = contenido;
    this.gerenciaFirmaTipo = tipo;
  }

  public void quitarFirmaComercial() {
    this.comercialFirma = null;
    this.comercialFirmaTipo = null;
  }

  public void quitarFirmaGerencia() {
    this.gerenciaFirma = null;
    this.gerenciaFirmaTipo = null;
  }
}
