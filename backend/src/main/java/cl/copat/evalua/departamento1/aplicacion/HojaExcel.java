package cl.copat.evalua.departamento1.aplicacion;

import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

final class HojaExcel {

  private final List<String> encabezados;
  private final List<Map<String, String>> filas;

  private HojaExcel(List<String> encabezados, List<Map<String, String>> filas) {
    this.encabezados = encabezados;
    this.filas = filas;
  }

  static HojaExcel leer(InputStream entrada) {
    DataFormatter formato = new DataFormatter(Locale.forLanguageTag("es-CL"));
    try (Workbook libro = WorkbookFactory.create(entrada)) {
      Sheet hoja = libro.getNumberOfSheets() == 0 ? null : libro.getSheetAt(0);
      if (hoja == null || hoja.getPhysicalNumberOfRows() == 0) {
        throw new ReglaNegocioException(Mensajes.EXCEL);
      }
      Row cabecera = hoja.getRow(hoja.getFirstRowNum());
      if (cabecera == null) {
        throw new ReglaNegocioException(Mensajes.EXCEL);
      }
      List<String> columnas = new ArrayList<>();
      for (int i = 0; i < cabecera.getLastCellNum(); i++) {
        columnas.add(clave(formato.formatCellValue(cabecera.getCell(i))));
      }
      List<Map<String, String>> filas = new ArrayList<>();
      for (int i = cabecera.getRowNum() + 1; i <= hoja.getLastRowNum(); i++) {
        Row fila = hoja.getRow(i);
        if (fila == null) {
          continue;
        }
        Map<String, String> valores = new LinkedHashMap<>();
        boolean vacia = true;
        for (int c = 0; c < columnas.size(); c++) {
          String texto = texto(formato, fila.getCell(c));
          if (!texto.isEmpty()) {
            vacia = false;
          }
          if (!columnas.get(c).isEmpty()) {
            valores.put(columnas.get(c), texto);
          }
        }
        if (!vacia) {
          valores.put("__fila", Integer.toString(i + 1));
          filas.add(valores);
        }
      }
      return new HojaExcel(columnas, filas);
    } catch (ReglaNegocioException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ReglaNegocioException(Mensajes.EXCEL);
    }
  }

  boolean tiene(String columna) {
    return encabezados.contains(columna);
  }

  List<Map<String, String>> filas() {
    return filas;
  }

  static String clave(String texto) {
    String base = texto == null ? "" : texto.trim();
    String sinAcento = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return sinAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  private static String texto(DataFormatter formato, Cell celda) {
    if (celda == null) {
      return "";
    }
    return formato.formatCellValue(celda).trim();
  }
}
