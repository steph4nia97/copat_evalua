package cl.copat.evalua.publico.dominio;

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

public final class InformeReclamo {

  private static final float MARGEN = 48;
  private static final float ANCHO = PDRectangle.A4.getWidth() - (MARGEN * 2);

  private InformeReclamo() {}

  public static String nombreArchivo(long id) {
    return "D-SIG-09-02-reclamo-" + id + ".pdf";
  }

  public static byte[] generar(long id, List<Fila> filas) {
    try (PDDocument documento = new PDDocument()) {
      PDFont fuente = fuente(documento);
      boolean unicode = fuente instanceof PDType0Font;
      Pagina pagina = new Pagina(documento, fuente, unicode);
      pagina.linea("Planilla de Seguimiento de Reclamos", 16);
      pagina.linea("D-SIG-09-02", 11);
      pagina.linea("Informe de cierre · Reclamo " + id, 12);
      pagina.espacio(8);
      String seccion = "";
      for (Fila fila : filas) {
        if (!fila.seccion().equals(seccion)) {
          seccion = fila.seccion();
          pagina.espacio(6);
          pagina.linea(seccion, 12);
        }
        String valor = fila.valor() == null || fila.valor().isBlank() ? "—" : fila.valor().trim();
        pagina.parrafo(fila.etiqueta() + ": " + valor, 10);
      }
      ByteArrayOutputStream salida = new ByteArrayOutputStream();
      documento.save(salida);
      return salida.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo generar el informe del reclamo", ex);
    }
  }

  private static PDFont fuente(PDDocument documento) throws IOException {
    Path arial = Path.of("C:/Windows/Fonts/arial.ttf");
    if (Files.exists(arial)) {
      return PDType0Font.load(documento, arial.toFile());
    }
    return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  }

  public record Fila(String seccion, String etiqueta, String valor) {}

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

    private void espacio(float alto) throws IOException {
      asegurar(alto);
      y -= alto;
    }

    private List<String> partir(String texto, float tamano) throws IOException {
      List<String> lineas = new ArrayList<>();
      String restante = texto.replace("\r", "");
      for (String parrafo : restante.split("\n", -1)) {
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
          .replace("¿", "").replace("—", "-").replace("·", "-");
    }
  }
}
