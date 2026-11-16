package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.aplicacion.AutenticacionService;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AutenticacionService autenticacion;
  private final UsuarioRepository usuarios;
  private final SecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();

  public AuthController(AutenticacionService autenticacion, UsuarioRepository usuarios) {
    this.autenticacion = autenticacion;
    this.usuarios = usuarios;
  }

  @PostMapping("/login")
  @Transactional(readOnly = true)
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
      Usuario usuario = usuarios.findById(principal.id()).orElseThrow();
      return ResponseEntity.ok(respuesta(usuario));
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
  @Transactional(readOnly = true)
  public UsuarioResponse me(Authentication authentication) {
    UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
    return respuesta(usuarios.findById(principal.id())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.UNAUTHORIZED, Mensajes.SESION)));
  }

  @GetMapping("/csrf")
  public ResponseEntity<Void> csrf() {
    return ResponseEntity.noContent().build();
  }

  private static UsuarioResponse respuesta(Usuario usuario) {
    short departamentoId = usuario.getDepartamento().getId();
    return new UsuarioResponse(
        usuario.getEmail(),
        usuario.getNombre(),
        Catalogo.nombreDepartamento(departamentoId),
        departamentoId,
        usuario.isClaveProvisoria());
  }

  private static ResponseEntity<ErrorRespuesta> noAutorizado() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta(Mensajes.CREDENCIALES));
  }
}
