package cl.copat.evalua.departamento2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento2.dominio.Desempeno;
import cl.copat.evalua.departamento2.dominio.GuiaPdf;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class GuiaPdfTest {

  @Test
  void laEncuestaEnBlancoNoEstaCompleta() {
    byte[] pdf = GuiaPdf.enBlanco(Catalogo.GUIA_1);
    assertThatThrownBy(() -> GuiaPdf.leer(pdf, Catalogo.GUIA_1))
        .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
          assertThat(error).hasMessage(Mensajes.PDF_INCOMPLETO);
          assertThat(error.getCampos()).containsKeys("nombre", "percepcion_general", "estado_obra");
          assertThat(error.getCampos()).doesNotContainKey("comentarios");
        });
  }

  @Test
  void ambasPlantillasLeenLaMismaEncuesta() {
    Map<String, String> valores = encuesta();
    byte[] pdf = GuiaPdf.conValores(Catalogo.GUIA_1, valores);
    assertThat(GuiaPdf.leer(pdf, Catalogo.GUIA_1).desempeno()).isEqualTo(Desempeno.SATISFACTORIO);
    assertThat(GuiaPdf.leer(pdf, Catalogo.GUIA_2).promedio()).isEqualByComparingTo("4.40");
    assertThat(GuiaPdf.leer(pdf, Catalogo.GUIA_1).estadoObra()).isEqualTo(GuiaPdf.OBRA_EN_CONSTRUCCION);
    assertThat(GuiaPdf.leer(pdf, Catalogo.GUIA_1).comentarios()).isEqualTo("Entrega puntual");
  }

  @Test
  void elComentarioEsOpcionalYElEstadoAceptaLaFormaSinAcento() {
    Map<String, String> valores = encuesta();
    valores.remove("comentarios");
    valores.put("estado_obra", "Obra en construccion");
    var lectura = GuiaPdf.leer(GuiaPdf.conValores(Catalogo.GUIA_2, valores), Catalogo.GUIA_2);
    assertThat(lectura.comentarios()).isNull();
    assertThat(lectura.estadoObra()).isEqualTo("Obra en construcción");
  }

  @Test
  void rechazaUnaNotaFueraDeRangoYUnArchivoQueNoEsPdf() throws Exception {
    Map<String, String> valores = encuesta();
    valores.put("percepcion_general", "6");
    assertThatThrownBy(() -> GuiaPdf.leer(GuiaPdf.conValores(Catalogo.GUIA_1, valores), Catalogo.GUIA_1))
        .isInstanceOfSatisfying(ReglaNegocioException.class, error ->
            assertThat(error.getCampos().get("percepcion_general")).contains("1 al 5"));

    valores.put("percepcion_general", "5");
    valores.put("estado_obra", "En pausa");
    assertThatThrownBy(() -> GuiaPdf.leer(GuiaPdf.conValores(Catalogo.GUIA_1, valores), Catalogo.GUIA_1))
        .isInstanceOfSatisfying(ReglaNegocioException.class, error ->
            assertThat(error.getCampos().get("estado_obra")).contains("Obra finalizada"));

    assertThatThrownBy(() -> GuiaPdf.leer("hola".getBytes(), Catalogo.GUIA_1))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessage(Mensajes.PDF_TIPO);

    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    try (PDDocument documento = new PDDocument()) {
      documento.addPage(new PDPage(PDRectangle.A4));
      documento.save(salida);
    }
    assertThatThrownBy(() -> GuiaPdf.leer(salida.toByteArray(), Catalogo.GUIA_1))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessage(Mensajes.PDF_GUIA);
  }

  @Test
  void elPromedioClasificaElDesempeno() {
    assertThat(Desempeno.calcular(5, 5, 5, 5, 5).desempeno()).isEqualTo(Desempeno.DESTACADO);
    assertThat(Desempeno.calcular(5, 5, 5, 4, 4).promedio()).isEqualByComparingTo("4.60");
    assertThat(Desempeno.calcular(5, 5, 5, 5, 4, 4, 4, 4, 4, 4).promedio()).isEqualByComparingTo("4.40");
    assertThat(Desempeno.calcular(5, 5, 5, 5, 4, 4, 4, 4, 4, 4).desempeno()).isEqualTo(Desempeno.SATISFACTORIO);
    assertThat(Desempeno.calcular(5, 5, 5, 5, 5, 4, 4, 4, 4, 4).desempeno()).isEqualTo(Desempeno.DESTACADO);
    assertThat(Desempeno.calcular(4, 4, 4, 3, 2).desempeno()).isEqualTo(Desempeno.REGULAR);
    assertThat(Desempeno.calcular(3, 3, 2, 2, 2).desempeno()).isEqualTo(Desempeno.INSUFICIENTE);
    assertThat(Desempeno.calcular(1, 1, 1, 1, 1, 1, 1, 1, 1, 1).promedio()).isEqualByComparingTo("1.00");
    assertThat(Desempeno.calcularOmitiendoVacias(5, null, 1, null).promedio()).isEqualByComparingTo("3.00");
    assertThat(Desempeno.calcularOmitiendoVacias(5, null, 1, null).desempeno()).isEqualTo(Desempeno.REGULAR);
  }

  static Map<String, String> encuesta() {
    Map<String, String> valores = new LinkedHashMap<>();
    valores.put("nombre", "Ana Soto");
    valores.put("empresa", "Norte Verde");
    valores.put("cargo", "Jefa de obra");
    valores.put("obra", "Edificio Costanera");
    valores.put("estado_obra", "Obra en construcción");
    valores.put("gestion_administrativa", "5");
    valores.put("tiempos_entrega", "5");
    valores.put("estado_hormigon", "5");
    valores.put("flexibilidad", "5");
    valores.put("atencion_comercial", "4");
    valores.put("respuesta_reclamos", "4");
    valores.put("entrega_informacion", "4");
    valores.put("competitividad_precio", "4");
    valores.put("capacitacion_tecnica", "4");
    valores.put("percepcion_general", "4");
    valores.put("comentarios", "Entrega puntual");
    return valores;
  }
}
