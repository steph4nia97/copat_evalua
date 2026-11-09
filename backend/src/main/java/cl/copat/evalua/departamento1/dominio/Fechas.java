package cl.copat.evalua.departamento1.dominio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class Fechas {

  public static final DateTimeFormatter FORMATO = DateTimeFormatter
      .ofPattern("dd/MM/uuuu")
      .withResolverStyle(ResolverStyle.STRICT);

  private Fechas() {}

  public static boolean esValida(String valor) {
    if (valor == null || !valor.matches("\\d{2}/\\d{2}/\\d{4}")) {
      return false;
    }
    try {
      LocalDate.parse(valor, FORMATO);
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public static LocalDate parse(String valor) {
    return LocalDate.parse(valor, FORMATO);
  }

  public static String format(LocalDate fecha) {
    return fecha.format(FORMATO);
  }
}
