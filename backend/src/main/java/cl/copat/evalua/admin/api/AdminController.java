package cl.copat.evalua.admin.api;

import cl.copat.evalua.admin.aplicacion.AdminService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
