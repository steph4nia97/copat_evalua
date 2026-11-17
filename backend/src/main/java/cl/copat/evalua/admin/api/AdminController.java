package cl.copat.evalua.admin.api;

import cl.copat.evalua.admin.aplicacion.AdminService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminService servicio;

  public AdminController(AdminService servicio) {
    this.servicio = servicio;
  }

  @GetMapping("/usuarios")
  public List<UsuarioCreadoResponse> usuarios(Authentication authentication) {
    return servicio.usuarios(principal(authentication));
  }

  @PostMapping("/usuarios")
  public UsuarioCreadoResponse crear(@RequestBody(required = false) CrearUsuarioRequest body, Authentication authentication) {
    return servicio.crear(body, principal(authentication));
  }

  @PutMapping("/usuarios/{id}")
  public UsuarioCreadoResponse editar(
      @PathVariable long id,
      @RequestBody(required = false) EditarUsuarioRequest body,
      Authentication authentication) {
    return servicio.editar(id, body, principal(authentication));
  }

  @DeleteMapping("/usuarios/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable long id, Authentication authentication) {
    servicio.eliminar(id, principal(authentication));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/usuarios/{id}/desactivar")
  public UsuarioCreadoResponse desactivar(@PathVariable long id, Authentication authentication) {
    return servicio.desactivar(id, principal(authentication));
  }

  @PostMapping("/usuarios/{id}/reactivar")
  public UsuarioCreadoResponse reactivar(@PathVariable long id, Authentication authentication) {
    return servicio.reactivar(id, principal(authentication));
  }

  @PutMapping("/usuarios/{id}/cargo")
  public UsuarioCreadoResponse cargo(
      @PathVariable long id,
      @RequestBody(required = false) AsignarCargoRequest body,
      Authentication authentication) {
    return servicio.asignarCargo(id, body, principal(authentication));
  }

  @GetMapping("/registros")
  public List<RegistroResponse> registros(
      @RequestParam(required = false) Short departamentoId,
      Authentication authentication) {
    return servicio.registros(departamentoId, principal(authentication));
  }

  private static UsuarioPrincipal principal(Authentication authentication) {
    return (UsuarioPrincipal) authentication.getPrincipal();
  }
}
