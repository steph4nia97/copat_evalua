package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.aplicacion.SolicitudService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departamento1")
public class SolicitudController {

  private final SolicitudService solicitudes;

  public SolicitudController(SolicitudService solicitudes) {
    this.solicitudes = solicitudes;
  }

  @GetMapping("/estados")
  public List<String> estados() {
    return solicitudes.estados();
  }

  @GetMapping("/plantillas")
  public List<String> plantillas() {
    return solicitudes.plantillas();
  }

  @GetMapping("/solicitudes")
  public List<SolicitudResponse> listar(@RequestParam(required = false) String estado, Authentication authentication) {
    return solicitudes.listar(estado, principal(authentication));
  }

  @PostMapping("/solicitudes")
  public ResponseEntity<SolicitudResponse> crear(
      @Valid @RequestBody CrearSolicitudRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED).body(solicitudes.crear(request, principal(authentication)));
  }

  @GetMapping("/bitacora")
  public List<BitacoraResponse> bitacora(Authentication authentication) {
    return solicitudes.bitacora(principal(authentication));
  }

  private static UsuarioPrincipal principal(Authentication authentication) {
    return (UsuarioPrincipal) authentication.getPrincipal();
  }
}
