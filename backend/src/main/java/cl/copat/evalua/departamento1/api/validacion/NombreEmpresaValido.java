package cl.copat.evalua.departamento1.api.validacion;

import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.NombreEmpresa;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = NombreEmpresaValido.Validador.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreEmpresaValido {

  String message() default Mensajes.NOMBRE;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validador implements ConstraintValidator<NombreEmpresaValido, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      if (value == null || value.isBlank()) {
        return Rechazo.de(context, Mensajes.NOMBRE_REQUERIDO);
      }
      if (!NombreEmpresa.esValido(NombreEmpresa.normalizar(value))) {
        return Rechazo.de(context, Mensajes.NOMBRE);
      }
      return true;
    }
  }
}
