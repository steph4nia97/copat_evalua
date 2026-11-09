package cl.copat.evalua.shared.web;

import cl.copat.evalua.departamento1.dominio.Mensajes;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.FieldError;

@RestControllerAdvice
public class ManejadorErrores {

  private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
    Map<String, String> campos = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      campos.putIfAbsent(error.getField(), error.getDefaultMessage());
    }
    return ResponseEntity.badRequest().body(new ErrorRespuesta(Mensajes.REVISE, campos));
  }

  @ExceptionHandler(ReglaNegocioException.class)
  public ResponseEntity<ErrorRespuesta> regla(ReglaNegocioException ex) {
    return ResponseEntity.badRequest().body(new ErrorRespuesta(ex.getMessage(), ex.getCampos()));
  }

  @ExceptionHandler(PeriodoDuplicadoException.class)
  public ResponseEntity<ErrorRespuesta> duplicado(PeriodoDuplicadoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespuesta(ex.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorRespuesta> cuerpo() {
    return ResponseEntity.badRequest().body(new ErrorRespuesta(Mensajes.CUERPO));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorRespuesta> estado(ResponseStatusException ex) {
    String mensaje = ex.getReason() == null ? Mensajes.INESPERADO : ex.getReason();
    return ResponseEntity.status(ex.getStatusCode()).body(new ErrorRespuesta(mensaje));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorRespuesta> noEncontrado() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(Mensajes.NO_ENCONTRADO));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorRespuesta> inesperado(Exception ex) {
    log.error("Error no controlado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorRespuesta(Mensajes.INESPERADO));
  }
}
