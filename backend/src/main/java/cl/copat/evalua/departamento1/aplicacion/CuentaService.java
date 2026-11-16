package cl.copat.evalua.departamento1.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CuentaService {

  private final UsuarioRepository usuarios;
  private final ActividadService actividad;
  private final PasswordEncoder passwordEncoder;

  public CuentaService(UsuarioRepository usuarios, ActividadService actividad, PasswordEncoder passwordEncoder) {
    this.usuarios = usuarios;
    this.actividad = actividad;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public Usuario renombrar(String nombre, String clave, UsuarioPrincipal principal) {
    Usuario usuario = cargar(principal);
    String claveIngresada = clave == null ? "" : clave;
    if (!passwordEncoder.matches(claveIngresada, usuario.getPasswordHash())) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("clave", Mensajes.CLAVE_ACTUAL));
    }
    String limpio = nombre == null ? "" : nombre.trim();
    if (limpio.isEmpty() || limpio.length() > 80) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("nombre", Mensajes.NOMBRE_USUARIO));
    }
    String anterior = usuario.getNombre();
    if (!anterior.equals(limpio)) {
      usuario.renombrar(limpio);
      usuarios.saveAndFlush(usuario);
      actividad.anotar(null, usuario, Catalogo.ACCION_NOMBRE, anterior + " ahora figura como " + limpio);
    }
    return usuario;
  }

  @Transactional
  public Usuario cambiarClave(String actual, String nueva, UsuarioPrincipal principal) {
    Usuario usuario = cargar(principal);
    String claveActual = actual == null ? "" : actual;
    String claveNueva = nueva == null ? "" : nueva;
    if (!passwordEncoder.matches(claveActual, usuario.getPasswordHash())) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("claveActual", Mensajes.CLAVE_ACTUAL));
    }
    if (claveNueva.length() < 8 || claveNueva.length() > 72) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("claveNueva", Mensajes.CLAVE_CORTA));
    }
    if (passwordEncoder.matches(claveNueva, usuario.getPasswordHash())) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("claveNueva", Mensajes.CLAVE_DISTINTA));
    }
    usuario.cambiarClave(passwordEncoder.encode(claveNueva));
    usuarios.saveAndFlush(usuario);
    actividad.anotar(null, usuario, Catalogo.ACCION_CLAVE, usuario.getNombre() + " actualizó su contraseña");
    return usuario;
  }

  private Usuario cargar(UsuarioPrincipal principal) {
    return usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
  }
}
