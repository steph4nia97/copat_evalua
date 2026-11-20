package cl.copat.evalua.publico.dominio;

import cl.copat.evalua.publico.api.GestionReclamosResponse;
import cl.copat.evalua.publico.api.GestionReclamosResponse.Corte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public final class InformeGestionPdf {

  private static final float MARGEN = 48;
  private static final float ANCHO = PDRectangle.A4.getWidth() - (MARGEN * 2);

  private InformeGestionPdf() {}

  public static String nombreArchivo(String desde, String hasta) {
    return "D-SIG-09-03-" + desde.replace("/", "-") + "_" + hasta.replace("/", "-") + ".pdf";
  }

  public static byte[] generar(GestionReclamosResponse datos, byte[] firmaComercial) {
    try (PDDocument documento = new PDDocument()) {
      PDFont fuente = fuente(documento);
      Pagina pagina = new Pagina(documento, fuente, fuente instanceof PDType0Font);
      pagina.linea("Informe de Gestión de Reclamos", 16);
      pagina.linea("D-SIG-09-03", 11);
      pagina.espacio(6);
      pagina.parrafo("Responsable de evaluación: " + valor(datos.responsable()), 10);
      pagina.parrafo("Cargo: " + valor(datos.cargo()), 10);
      pagina.parrafo("Periodo evaluado: " + datos.desde() + " – " + datos.hasta(), 10);
      pagina.parrafo("Aprobado por: " + valor(datos.aprobadoPor()), 10);
      pagina.espacio(6);
      pagina.linea("Resumen ejecutivo", 12);
      pagina.parrafo("Número total de reclamos: " + datos.total(), 10);
      pagina.parrafo("Principales causas:\n" + valor(datos.principalesCausas()), 10);
      pagina.parrafo("Cumplimiento de plazos: " + valor(datos.cumplimientoPlazos()), 10);
      pagina.espacio(6);
      pagina.linea("Gráficos", 12);
      pagina.parrafo("% de reclamos por tipo:\n" + cortes(datos.porTipo()), 10);
      pagina.parrafo("% de reclamos por área:\n" + cortes(datos.porArea()), 10);
      pagina.parrafo("Tiempo promedio de cierre: " + datos.tiempoPromedio() + " días", 10);
      pagina.parrafo("Reclamos reincidentes: " + datos.reincidentes() + " (" + datos.porcentajeReincidentes() + "%)", 10);
      pagina.espacio(6);
      pagina.linea("Seguimiento de indicadores", 12);
      pagina.parrafo("% de reclamos cerrados ≤ 5 días. Meta ≥ 90%. Resultado " + datos.porcentajeCierre() + "%. Estado " + datos.estadoCierre() + ".", 10);
      pagina.parrafo("Tiempo promedio de cierre. Meta ≤ 5 días. Resultado " + datos.tiempoPromedio() + " días. Estado " + datos.estadoTiempo() + ".", 10);
      pagina.parrafo("% de reclamos reincidentes. Meta ≤ 5%. Resultado " + datos.porcentajeReincidentes() + "%. Estado " + datos.estadoReincidentes() + ".", 10);
      pagina.espacio(6);
      pagina.linea("Análisis y evaluación de reclamos", 12);
      pagina.parrafo("Causas recurrentes: " + valor(datos.causasRecurrentes()), 10);
      pagina.parrafo("Áreas con mayor frecuencia de reclamos: " + valor(datos.areasFrecuencia()), 10);
      pagina.parrafo("Efectividad de acciones correctivas: " + valor(datos.efectividad()), 10);
      pagina.espacio(6);
      pagina.linea("Conclusiones y acciones de mejora", 12);
      pagina.parrafo(valor(datos.conclusiones()), 10);
      pagina.espacio(6);
      pagina.linea("Firma y aprobación", 12);
      pagina.parrafo(firma("Subgerente comercial", datos.comercialNombre(), datos.comercialCargo(), datos.comercialFecha(), datos.comercialFirmaVirtual()), 10);
      pagina.firmaImagen(datos.comercialFirmaVirtual() ? firmaComercial : null);
      ByteArrayOutputStream salida = new ByteArrayOutputStream();
      documento.save(salida);
      return salida.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo generar el informe de gestión", ex);
    }
  }

  private static String cortes(List<Corte> cortes) {
    if (cortes == null || cortes.isEmpty()) {
      return "—";
    }
    List<String> lineas = new ArrayList<>();
    for (Corte corte : cortes) {
      lineas.add(corte.nombre() + ": " + corte.cantidad() + " (" + corte.porcentaje() + "%)");
    }
    return String.join("\n", lineas);
  }

  private static String firma(String rol, String nombre, String cargo, String fecha, boolean virtual) {
    return rol + ". Persona: " + valor(nombre) + ". Perfil: " + valor(cargo) + ". Fecha: " + valor(fecha)
        + ". Firma: " + (virtual ? "Firma virtual" : "Pendiente") + ".";
  }

  private static String valor(String texto) {
    return texto == null || texto.isBlank() ? "—" : texto.trim();
  }

  private static PDFont fuente(PDDocument documento) throws IOException {
    Path arial = Path.of("C:/Windows/Fonts/arial.ttf");
    if (Files.exists(arial)) {
      return PDType0Font.load(documento, arial.toFile());
    }
    return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  }

  private static final class Pagina {
    private final PDDocument documento;
    private final PDFont fuente;
    private final boolean unicode;
    private PDPage hoja;
    private float y;

    private Pagina(PDDocument documento, PDFont fuente, boolean unicode) throws IOException {
      this.documento = documento;
      this.fuente = fuente;
      this.unicode = unicode;
      nueva();
    }

    private void linea(String texto, float tamano) throws IOException {
      asegurar(tamano + 8);
      escribir(texto, tamano);
    }

    private void parrafo(String texto, float tamano) throws IOException {
      for (String linea : partir(texto, tamano)) {
        linea(linea, tamano);
      }
      y -= 2;
    }

    private void firmaImagen(byte[] bytes) throws IOException {
      if (bytes == null || bytes.length == 0) {
        return;
      }
      PDImageXObject imagen = PDImageXObject.createFromByteArray(documento, bytes, "firma");
      float escala = Math.min(180f / imagen.getWidth(), 64f / imagen.getHeight());
      float ancho = imagen.getWidth() * escala;
      float alto = imagen.getHeight() * escala;
      asegurar(alto + 6);
      try (PDPageContentStream contenido = new PDPageContentStream(
          documento, hoja, PDPageContentStream.AppendMode.APPEND, true)) {
        contenido.drawImage(imagen, MARGEN, y - alto, ancho, alto);
      }
      y -= alto + 6;
    }

    private void espacio(float alto) throws IOException {
      asegurar(alto);
      y -= alto;
    }

    private List<String> partir(String texto, float tamano) throws IOException {
      List<String> lineas = new ArrayList<>();
      for (String parrafo : texto.replace("\r", "").split("\n", -1)) {
        StringBuilder actual = new StringBuilder();
        for (String palabra : parrafo.split(" ")) {
          String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
          if (!actual.isEmpty() && ancho(candidato, tamano) > ANCHO) {
            lineas.add(actual.toString());
            actual = new StringBuilder(palabra);
          } else {
            actual = new StringBuilder(candidato);
          }
        }
        lineas.add(actual.toString());
      }
      return lineas;
    }

    private float ancho(String texto, float tamano) throws IOException {
      return fuente.getStringWidth(visible(texto)) / 1000 * tamano;
    }

    private void asegurar(float alto) throws IOException {
      if (y - alto < MARGEN) {
        nueva();
      }
    }

    private void nueva() throws IOException {
      hoja = new PDPage(PDRectangle.A4);
      documento.addPage(hoja);
      y = PDRectangle.A4.getHeight() - MARGEN;
    }

    private void escribir(String texto, float tamano) throws IOException {
      try (PDPageContentStream contenido = new PDPageContentStream(
          documento, hoja, PDPageContentStream.AppendMode.APPEND, true)) {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(MARGEN, y);
        contenido.showText(visible(texto));
        contenido.endText();
      }
      y -= tamano + 4;
    }

    private String visible(String texto) {
      if (unicode) {
        return texto;
      }
      return texto
          .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
          .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
          .replace("ñ", "n").replace("Ñ", "N").replace("ü", "u").replace("Ü", "U")
          .replace("≤", "<=").replace("≥", ">=").replace("–", "-").replace("—", "-");
    }
  }
}
