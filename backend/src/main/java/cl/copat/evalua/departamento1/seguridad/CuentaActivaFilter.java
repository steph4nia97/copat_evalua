package cl.copat.evalua.departamento1.seguridad;

import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.shared.web.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CuentaActivaFilter extends OncePerRequestFilter {

  private final UsuarioRepository usuarios;
  private final ObjectMapper json;

  public CuentaActivaFilter(UsuarioRepository usuarios, ObjectMapper json) {
    this.usuarios = usuarios;
    this.json = json;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
      boolean activa = usuarios.findById(principal.id()).map(usuario -> usuario.isActivo()).orElse(false);
      if (!activa) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
          session.invalidate();
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), new ErrorRespuesta(Mensajes.CUENTA_DESACTIVADA));
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
