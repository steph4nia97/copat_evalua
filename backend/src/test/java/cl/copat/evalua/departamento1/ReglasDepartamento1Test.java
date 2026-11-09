package cl.copat.evalua.departamento1;

import static org.assertj.core.api.Assertions.assertThat;

import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.NombreEmpresa;
import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.dominio.TelefonoChile;
import org.junit.jupiter.api.Test;

class ReglasDepartamento1Test {

  @Test
  void validaRutConModulo11() {
    assertThat(Rut.esValido("12.345.678-5")).isTrue();
    assertThat(Rut.esValido("12345678-5")).isTrue();
    assertThat(Rut.esValido("11111111-1")).isTrue();
    assertThat(Rut.esValido("76.086.428-5")).isTrue();
    assertThat(Rut.esValido("12345678-k")).isFalse();
    assertThat(Rut.esValido("12.345.678-9")).isFalse();
    assertThat(Rut.esValido("123")).isFalse();
    assertThat(Rut.normalizar("12.345.678-5")).isEqualTo("12345678-5");
    assertThat(Rut.formatear("12345678-5")).isEqualTo("12.345.678-5");
    assertThat(Rut.formatear("11111111-1")).isEqualTo("11.111.111-1");
  }

  @Test
  void validaNombreEmpresa() {
    assertThat(NombreEmpresa.esValido(NombreEmpresa.normalizar("Empresa 1"))).isTrue();
    assertThat(NombreEmpresa.esValido(NombreEmpresa.normalizar("  Ñuñoa   12  "))).isTrue();
    assertThat(NombreEmpresa.normalizar("  Ñuñoa   12  ")).isEqualTo("Ñuñoa 12");
    assertThat(NombreEmpresa.esValido(NombreEmpresa.normalizar("Empresa-1"))).isFalse();
    assertThat(NombreEmpresa.esValido(NombreEmpresa.normalizar(""))).isFalse();
    assertThat(NombreEmpresa.esValido("A".repeat(101))).isFalse();
    assertThat(NombreEmpresa.esValido("A".repeat(100))).isTrue();
  }

  @Test
  void validaTelefonoChilenoSoloDigitos() {
    assertThat(TelefonoChile.esValido(null)).isTrue();
    assertThat(TelefonoChile.esValido("  ")).isTrue();
    assertThat(TelefonoChile.esValido("912345678")).isTrue();
    assertThat(TelefonoChile.esValido("221234567")).isTrue();
    assertThat(TelefonoChile.esValido("56912345678")).isTrue();
    assertThat(TelefonoChile.esValido("+56912345678")).isFalse();
    assertThat(TelefonoChile.esValido("9 1234 5678")).isFalse();
    assertThat(TelefonoChile.esValido("123456789")).isFalse();
    assertThat(TelefonoChile.esValido("91234567")).isFalse();
    assertThat(TelefonoChile.normalizar(" ")).isNull();
  }

  @Test
  void validaFechasDiaMesAnio() {
    assertThat(Fechas.esValida("01/01/2026")).isTrue();
    assertThat(Fechas.esValida("29/02/2024")).isTrue();
    assertThat(Fechas.esValida("29/02/2026")).isFalse();
    assertThat(Fechas.esValida("31/04/2026")).isFalse();
    assertThat(Fechas.esValida("2026-01-01")).isFalse();
    assertThat(Fechas.esValida("1/1/2026")).isFalse();
    assertThat(Fechas.parse("31/01/2026").isAfter(Fechas.parse("01/01/2026"))).isTrue();
  }
}
