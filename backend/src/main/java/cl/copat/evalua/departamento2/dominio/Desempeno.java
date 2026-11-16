package cl.copat.evalua.departamento2.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public final class Desempeno {

  public static final String DESTACADO = "Destacado";
  public static final String SATISFACTORIO = "Satisfactorio";
  public static final String REGULAR = "Regular";
  public static final String INSUFICIENTE = "Insuficiente";
  public static final String ESCALA =
      "Promedio de las satisfacciones del 1 al 5. No aplica no entra al promedio. Destacado desde 4,50. Satisfactorio desde 3,50. Regular desde 2,50. Bajo 2,50, insuficiente.";

  private Desempeno() {}

  public static Resultado calcularOmitiendoVacias(Integer... notas) {
    if (notas == null) {
      throw new IllegalArgumentException("La evaluación necesita al menos una nota");
    }
    int[] utiles = Arrays.stream(notas).filter(nota -> nota != null).mapToInt(Integer::intValue).toArray();
    return calcular(utiles);
  }

  public static Resultado calcular(int... notas) {
    if (notas == null || notas.length == 0) {
      throw new IllegalArgumentException("La evaluación necesita al menos una nota");
    }
    int suma = Arrays.stream(notas).sum();
    BigDecimal promedio = BigDecimal.valueOf(suma)
        .divide(BigDecimal.valueOf(notas.length), 2, RoundingMode.HALF_UP);
    String nivel;
    if (promedio.compareTo(new BigDecimal("4.50")) >= 0) {
      nivel = DESTACADO;
    } else if (promedio.compareTo(new BigDecimal("3.50")) >= 0) {
      nivel = SATISFACTORIO;
    } else if (promedio.compareTo(new BigDecimal("2.50")) >= 0) {
      nivel = REGULAR;
    } else {
      nivel = INSUFICIENTE;
    }
    return new Resultado(promedio, nivel);
  }

  public record Resultado(BigDecimal promedio, String desempeno) {}
}
