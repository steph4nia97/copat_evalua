package cl.copat.evalua.publico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import cl.copat.evalua.publico.api.GestionReclamosResponse.Corte;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PorcentajeAreaTest {

  @Test
  void cadaAreaSeleccionadaCuentaPorSeparado() {
    List<Corte> cortes = GestionReclamosService.porcentajesDeArea(List.of(
        "Calidad",
        "Ventas",
        "Ventas, Calidad, Operaciones, Administración, Finanzas",
        "despacho"));

    assertThat(cortes).extracting(Corte::nombre)
        .containsExactly("Calidad", "Ventas", "Administración", "Finanzas", "Operaciones", "despacho");
    assertThat(linea(cortes, "Calidad")).isEqualTo("2 (25,0%)");
    assertThat(linea(cortes, "Ventas")).isEqualTo("2 (25,0%)");
    assertThat(linea(cortes, "Operaciones")).isEqualTo("1 (12,5%)");
    assertThat(linea(cortes, "Administración")).isEqualTo("1 (12,5%)");
    assertThat(linea(cortes, "Finanzas")).isEqualTo("1 (12,5%)");
    assertThat(linea(cortes, "despacho")).isEqualTo("1 (12,5%)");
  }

  @Test
  void unReclamoSinAreaSigueSiendoUnItem() {
    List<Corte> cortes = GestionReclamosService.porcentajesDeArea(Arrays.asList("Calidad", null));

    assertThat(linea(cortes, "Calidad")).isEqualTo("1 (50,0%)");
    assertThat(linea(cortes, "Sin área")).isEqualTo("1 (50,0%)");
  }

  private static String linea(List<Corte> cortes, String nombre) {
    Corte corte = cortes.stream().filter(item -> item.nombre().equals(nombre)).findFirst().orElseThrow();
    return corte.cantidad() + " (" + corte.porcentaje() + "%)";
  }
}
