package cl.copat.evalua.departamento1.api.validacion;

import jakarta.validation.ConstraintValidatorContext;

final class Rechazo {

  private Rechazo() {}

  static boolean de(ConstraintValidatorContext context, String mensaje) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
    return false;
  }
}
