package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.aplicacion.AutenticacionService;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.shared.web.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AutenticacionService autenticacion;
  private final SecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();

  public AuthController(AutenticacionService autenticacion) {
    this.autenticacion = autenticacion;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(
      @RequestBody(required = false) LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (body == null || body.correo() == null || body.correo().isBlank()
        || body.contrasena() == null || body.contrasena().isBlank()) {
      return noAutorizado();
    }
    try {
      UsuarioPrincipal principal = autenticacion.autenticar(body.correo(), body.contrasena());
      UsernamePasswordAuthenticationToken autenticado = UsernamePasswordAuthenticationToken.authenticated(
          principal, null, principal.getAuthorities());
      SecurityContext contexto = SecurityContextHolder.createEmptyContext();
      contexto.setAuthentication(autenticado);
      SecurityContextHolder.setContext(contexto);
      request.getSession(true);
      request.changeSessionId();
      sesiones.saveContext(contexto, request, response);
      return ResponseEntity.ok(respuesta(principal));
    } catch (BadCredentialsException ex) {
      SecurityContextHolder.clearContext();
      return noAutorizado();
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public UsuarioResponse me(Authentication authentication) {
    return respuesta((UsuarioPrincipal) authentication.getPrincipal());
  }

  @GetMapping("/csrf")
  public ResponseEntity<Void> csrf() {
    return ResponseEntity.noContent().build();
  }

  private static UsuarioResponse respuesta(UsuarioPrincipal principal) {
    return new UsuarioResponse(principal.email(), principal.nombre(), "Departamento 1");
  }

  private static ResponseEntity<ErrorRespuesta> noAutorizado() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta(Mensajes.CREDENCIALES));
  }
}
