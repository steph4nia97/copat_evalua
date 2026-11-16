package cl.copat.evalua.departamento1.api.validacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@Documented
@Constraint(validatedBy = SedesValidas.Validador.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SedesValidas {

  String message() default Mensajes.SEDE;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validador implements ConstraintValidator<SedesValidas, List<String>> {

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
      if (value == null || value.stream().noneMatch(sede -> sede != null && !sede.isBlank())) {
        return Rechazo.de(context, Mensajes.SEDE_REQUERIDA);
      }
      boolean conocida = value.stream()
          .filter(sede -> sede != null && !sede.isBlank())
          .map(String::trim)
          .allMatch(Catalogo.SEDES::contains);
      if (!conocida) {
        return Rechazo.de(context, Mensajes.SEDE);
      }
      return true;
    }
  }
}
