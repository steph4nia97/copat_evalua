package cl.copat.evalua.departamento2.dominio;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento2.dominio.Desempeno.Resultado;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

public final class GuiaPdf {

  public static final String ARCHIVO = "D-SIG-10-01-encuesta-satisfaccion.pdf";
  public static final String OBRA_FINALIZADA = "Obra finalizada";
  public static final String OBRA_EN_CONSTRUCCION = "Obra en construcción";

  public static final List<Pregunta> NOTAS = List.of(
      new Pregunta("gestion_administrativa", "Gestión administrativa"),
      new Pregunta("tiempos_entrega", "Cumplimiento de los tiempos de entrega"),
      new Pregunta("estado_hormigon", "Estado del hormigón"),
      new Pregunta("flexibilidad", "Flexibilidad en los cambios de programación"),
      new Pregunta("atencion_comercial", "Atención y soporte del equipo comercial"),
      new Pregunta("respuesta_reclamos", "Respuesta a problemas o reclamos"),
      new Pregunta("entrega_informacion", "Entrega de información"),
      new Pregunta("competitividad_precio", "Competitividad del precio"),
      new Pregunta("capacitacion_tecnica", "Capacitación técnica y asesoramiento"),
      new Pregunta("percepcion_general", "Percepción general del servicio"));

  public static final Set<String> SIN_NOTA = Set.of("flexibilidad", "respuesta_reclamos");

  private static final List<Campo> CAMPOS = campos();

  private GuiaPdf() {}

  public static boolean admiteSinNota(String clave) {
    return SIN_NOTA.contains(clave);
  }

  public static String nombreArchivo(String plantilla) {
    exigirPlantilla(plantilla);
    return ARCHIVO;
  }

  public static byte[] enBlanco(String plantilla) {
    return dibujar(plantilla, Map.of());
  }

  public static byte[] conValores(String plantilla, Map<String, String> valores) {
    return dibujar(plantilla, valores == null ? Map.of() : valores);
  }

  public static Lectura leer(byte[] pdf, String plantilla) {
    exigirPlantilla(plantilla);
    if (pdf == null || pdf.length < 5 || !esPdf(pdf)) {
      throw new ReglaNegocioException(Mensajes.PDF_TIPO);
    }
    try (PDDocument documento = Loader.loadPDF(pdf)) {
      PDAcroForm formulario = documento.getDocumentCatalog().getAcroForm();
      if (formulario == null) {
        throw new ReglaNegocioException(Mensajes.PDF_GUIA);
      }
      Map<String, String> errores = new LinkedHashMap<>();
      Map<String, String> textos = new LinkedHashMap<>();
      Map<String, Integer> notas = new LinkedHashMap<>();
      for (Campo campo : CAMPOS) {
        PDField hallado = formulario.getField(campo.clave());
        String valor = hallado == null ? "" : texto(hallado.getValueAsString());
        if (campo.tipo() == Tipo.COMENTARIO) {
          if (valor.length() > campo.maximo()) {
            errores.put(campo.clave(), campo.etiqueta() + ": admite como máximo " + campo.maximo() + " caracteres.");
          } else if (!valor.isEmpty()) {
            textos.put(campo.clave(), valor);
          }
          continue;
        }
        if (valor.isEmpty()) {
          errores.put(campo.clave(), campo.etiqueta() + ": complete este campo.");
          continue;
        }
        if (valor.length() > campo.maximo()) {
          errores.put(campo.clave(), campo.etiqueta() + ": admite como máximo " + campo.maximo() + " caracteres.");
          continue;
        }
        if (campo.tipo() == Tipo.NOTA) {
          if (!valor.matches("[1-5]")) {
            errores.put(campo.clave(), campo.etiqueta() + ": use un número del 1 al 5.");
          } else {
            notas.put(campo.clave(), Integer.parseInt(valor));
          }
          continue;
        }
        if (campo.tipo() == Tipo.ESTADO) {
          String estado = estado(valor);
          if (estado == null) {
            errores.put(campo.clave(), campo.etiqueta() + ": escriba Obra finalizada u Obra en construcción.");
          } else {
            textos.put(campo.clave(), estado);
          }
          continue;
        }
        textos.put(campo.clave(), valor);
      }
      if (!errores.isEmpty()) {
        throw new ReglaNegocioException(Mensajes.PDF_INCOMPLETO, errores);
      }
      int[] valores = NOTAS.stream().mapToInt(pregunta -> notas.get(pregunta.clave())).toArray();
      Resultado resultado = Desempeno.calcular(valores);
      return new Lectura(
          textos.get("nombre"),
          textos.get("empresa"),
          textos.get("cargo"),
          textos.get("obra"),
          textos.get("estado_obra"),
          textos.get("comentarios"),
          notas,
          resultado.promedio(),
          resultado.desempeno());
    } catch (ReglaNegocioException ex) {
      throw ex;
    } catch (IOException ex) {
      throw new ReglaNegocioException(Mensajes.PDF_GUIA);
    }
  }

  private static byte[] dibujar(String plantilla, Map<String, String> valores) {
    exigirPlantilla(plantilla);
    try (PDDocument documento = new PDDocument()) {
      PDFont fuente = fuente(documento);
      boolean unicode = fuente instanceof PDType0Font;
      PDAcroForm formulario = new PDAcroForm(documento);
      documento.getDocumentCatalog().setAcroForm(formulario);
      formulario.setNeedAppearances(true);
      PDResources recursos = new PDResources();
      recursos.put(COSName.getPDFName("Helv"), new PDType1Font(Standard14Fonts.FontName.HELVETICA));
      formulario.setDefaultResources(recursos);
      formulario.setDefaultAppearance("/Helv 10 Tf 0 g");

      Cursor cursor = new Cursor(documento, fuente, unicode);
      cursor.titulo("ANEXO I");
      cursor.linea("Encuesta de Satisfaccion del Cliente", 14);
      cursor.linea("D-SIG-10-01", 11);
      cursor.linea("Datos generales", 12);
      for (Campo campo : CAMPOS) {
        if ("gestion_administrativa".equals(campo.clave())) {
          cursor.linea("Satisfaccion del 1 al 5", 12);
          cursor.linea("1 Muy insatisfecho. 2 Insatisfecho. 3 Indiferente. 4 Satisfecho. 5 Muy satisfecho.", 9);
        }
        cursor.campo(formulario, campo, valores.get(campo.clave()));
      }

      ByteArrayOutputStream salida = new ByteArrayOutputStream();
      documento.save(salida);
      return salida.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo generar la encuesta", ex);
    }
  }

  private static PDFont fuente(PDDocument documento) throws IOException {
    Path arial = Path.of("C:/Windows/Fonts/arial.ttf");
    if (Files.exists(arial)) {
      return PDType0Font.load(documento, arial.toFile());
    }
    return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  }

  private static void exigirPlantilla(String plantilla) {
    if (!Catalogo.PLANTILLAS.contains(plantilla)) {
      throw new IllegalStateException("Plantilla sin guía");
    }
  }

  private static boolean esPdf(byte[] pdf) {
    return pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F';
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor.trim();
  }

  private static String estado(String valor) {
    String normal = valor.toLowerCase(Locale.ROOT).replace('ó', 'o').replace('Ó', 'o');
    if (normal.equals("obra finalizada")) {
      return OBRA_FINALIZADA;
    }
    if (normal.equals("obra en construccion")) {
      return OBRA_EN_CONSTRUCCION;
    }
    return null;
  }

  private static String ascii(String texto) {
    return texto
        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
        .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
        .replace("ñ", "n").replace("Ñ", "N").replace("ü", "u").replace("Ü", "U")
        .replace("¿", "").replace("?", "");
  }

  private static List<Campo> campos() {
    List<Campo> campos = new ArrayList<>();
    campos.add(new Campo("nombre", "¿Cuál es tu nombre?", Tipo.TEXTO, 120));
    campos.add(new Campo("empresa", "¿En qué empresa trabaja?", Tipo.TEXTO, 150));
    campos.add(new Campo("cargo", "¿Cuál es su cargo?", Tipo.TEXTO, 120));
    campos.add(new Campo("obra", "¿Cuál es el nombre de la obra en la que trabaja o trabajó?", Tipo.TEXTO, 200));
    campos.add(new Campo(
        "estado_obra",
        "Estado de la obra: Obra finalizada u Obra en construcción",
        Tipo.ESTADO,
        40));
    campos.add(new Campo("gestion_administrativa", "Gestión administrativa", Tipo.NOTA, 1));
    campos.add(new Campo("tiempos_entrega", "Cumplimiento de los tiempos de entrega", Tipo.NOTA, 1));
    campos.add(new Campo("estado_hormigon", "Estado del hormigón", Tipo.NOTA, 1));
    campos.add(new Campo("flexibilidad", "Flexibilidad en los cambios de programación", Tipo.NOTA, 1));
    campos.add(new Campo("atencion_comercial", "Atención y soporte del equipo comercial", Tipo.NOTA, 1));
    campos.add(new Campo("respuesta_reclamos", "Respuesta a problemas o reclamos", Tipo.NOTA, 1));
    campos.add(new Campo("entrega_informacion", "Entrega de información", Tipo.NOTA, 1));
    campos.add(new Campo("competitividad_precio", "Competitividad del precio", Tipo.NOTA, 1));
    campos.add(new Campo("capacitacion_tecnica", "Capacitación técnica y asesoramiento", Tipo.NOTA, 1));
    campos.add(new Campo("percepcion_general", "Percepción general del servicio", Tipo.NOTA, 1));
    campos.add(new Campo("comentarios", "Comentarios adicionales", Tipo.COMENTARIO, 1000));
    return List.copyOf(campos);
  }

  public record Pregunta(String clave, String etiqueta) {}

  public record Lectura(
      String nombre,
      String empresa,
      String cargo,
      String obra,
      String estadoObra,
      String comentarios,
      Map<String, Integer> notas,
      BigDecimal promedio,
      String desempeno) {}

  private enum Tipo {
    TEXTO, NOTA, ESTADO, COMENTARIO
  }

  private record Campo(String clave, String etiqueta, Tipo tipo, int maximo) {}

  private static final class Cursor {
    private final PDDocument documento;
    private final PDFont fuente;
    private final boolean unicode;
    private PDPage pagina;
    private float y;

    private Cursor(PDDocument documento, PDFont fuente, boolean unicode) throws IOException {
      this.documento = documento;
      this.fuente = fuente;
      this.unicode = unicode;
      nueva();
    }

    private void titulo(String texto) throws IOException {
      escribir(texto, 16);
      y -= 8;
    }

    private void linea(String texto, float tamano) throws IOException {
      asegurar(tamano + 8);
      escribir(texto, tamano);
    }

    private void campo(PDAcroForm formulario, Campo campo, String valor) throws IOException {
      float alto = campo.tipo() == Tipo.COMENTARIO ? 48 : 16;
      asegurar(alto + 28);
      escribir(campo.etiqueta(), 9);
      PDTextField texto = new PDTextField(formulario);
      texto.setPartialName(campo.clave());
      texto.setDefaultAppearance("/Helv 10 Tf 0 g");
      if (campo.tipo() == Tipo.COMENTARIO) {
        texto.setMultiline(true);
      }
      formulario.getFields().add(texto);
      PDAnnotationWidget widget = texto.getWidgets().isEmpty() ? new PDAnnotationWidget() : texto.getWidgets().get(0);
      if (texto.getWidgets().isEmpty()) {
        texto.getWidgets().add(widget);
      }
      widget.setRectangle(new PDRectangle(48, y - alto, 500, alto));
      widget.setPage(pagina);
      widget.setPrinted(true);
      pagina.getAnnotations().add(widget);
      if (valor != null && !valor.isBlank()) {
        texto.getCOSObject().setString(COSName.V, valor.trim());
      }
      y -= alto + 14;
    }

    private void asegurar(float alto) throws IOException {
      if (y - alto < 48) {
        nueva();
      }
    }

    private void nueva() throws IOException {
      pagina = new PDPage(PDRectangle.A4);
      documento.addPage(pagina);
      y = 800;
    }

    private void escribir(String texto, float tamano) throws IOException {
      try (PDPageContentStream contenido = new PDPageContentStream(
          documento, pagina, PDPageContentStream.AppendMode.APPEND, true)) {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(48, y);
        contenido.showText(unicode ? texto : ascii(texto));
        contenido.endText();
      }
      y -= tamano + 6;
    }
  }
}
