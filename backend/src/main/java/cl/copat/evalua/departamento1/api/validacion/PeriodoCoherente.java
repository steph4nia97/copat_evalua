package cl.copat.evalua.departamento1.api.validacion;

import cl.copat.evalua.departamento1.api.CrearSolicitudRequest;
import cl.copat.evalua.departamento1.dominio.Fechas;
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
import java.time.LocalDate;

@Documented
@Constraint(validatedBy = PeriodoCoherente.Validador.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PeriodoCoherente {

  String message() default Mensajes.PERIODO;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validador implements ConstraintValidator<PeriodoCoherente, CrearSolicitudRequest> {

    @Override
    public boolean isValid(CrearSolicitudRequest value, ConstraintValidatorContext context) {
      if (value == null) {
        return true;
      }
      String inicioTexto = value.periodoInicio() == null ? null : value.periodoInicio().trim();
      String finTexto = value.periodoFin() == null ? null : value.periodoFin().trim();
      if (!Fechas.esValida(inicioTexto) || !Fechas.esValida(finTexto)) {
        return true;
      }
      LocalDate inicio = Fechas.parse(inicioTexto);
      LocalDate fin = Fechas.parse(finTexto);
      if (!fin.isBefore(inicio)) {
        return true;
      }
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(Mensajes.PERIODO)
          .addPropertyNode("periodoFin")
          .addConstraintViolation();
      return false;
    }
  }
}
