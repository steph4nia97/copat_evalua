package cl.copat.evalua.shared.web;

import cl.copat.evalua.departamento1.dominio.Mensajes;

public class PeriodoDuplicadoException extends RuntimeException {

  public PeriodoDuplicadoException() {
    super(Mensajes.DUPLICADO);
  }
}
