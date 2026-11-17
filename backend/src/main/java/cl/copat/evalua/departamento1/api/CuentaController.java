package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.aplicacion.CuentaService;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

  private final CuentaService cuenta;
  private final SecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();

  public CuentaController(CuentaService cuenta) {
    this.cuenta = cuenta;
  }

  @PutMapping("/nombre")
  @Transactional
  public UsuarioResponse nombre(
      @RequestBody(required = false) PerfilRequest body,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    Usuario usuario = cuenta.renombrar(
        body == null ? null : body.nombre(),
        body == null ? null : body.clave(),
        principal(authentication));
    refrescar(usuario, request, response);
    return respuesta(usuario);
  }

  @PutMapping("/clave")
  @Transactional
  public UsuarioResponse clave(
      @RequestBody(required = false) ClaveRequest body,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    Usuario usuario = cuenta.cambiarClave(
        body == null ? null : body.claveActual(),
        body == null ? null : body.claveNueva(),
        principal(authentication));
    refrescar(usuario, request, response);
    return respuesta(usuario);
  }

  private void refrescar(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
    UsuarioPrincipal principal = new UsuarioPrincipal(
        usuario.getId(),
        usuario.getEmail(),
        usuario.getNombre(),
        usuario.getDepartamento().getId());
    UsernamePasswordAuthenticationToken autenticado = UsernamePasswordAuthenticationToken.authenticated(
        principal, null, principal.getAuthorities());
    SecurityContext contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(autenticado);
    SecurityContextHolder.setContext(contexto);
    sesiones.saveContext(contexto, request, response);
  }

  private static UsuarioResponse respuesta(Usuario usuario) {
    short departamentoId = usuario.getDepartamento().getId();
    return new UsuarioResponse(
        usuario.getEmail(),
        usuario.getNombre(),
        usuario.getCargo(),
        Catalogo.nombreDepartamento(departamentoId),
        departamentoId,
        usuario.isClaveProvisoria());
  }

  private static UsuarioPrincipal principal(Authentication authentication) {
    return (UsuarioPrincipal) authentication.getPrincipal();
  }
}
