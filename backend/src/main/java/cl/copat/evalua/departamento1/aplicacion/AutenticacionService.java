package cl.copat.evalua.departamento1.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

  private final UsuarioRepository usuarios;
  private final PasswordEncoder passwordEncoder;
  private final String hashFicticio;

  public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
    this.usuarios = usuarios;
    this.passwordEncoder = passwordEncoder;
    this.hashFicticio = passwordEncoder.encode("dummy-copat-no-usuario");
  }

  public UsuarioPrincipal autenticar(String correo, String contrasena) {
    String email = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    String clave = contrasena == null ? "" : contrasena;
    Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
    if (usuario == null || usuario.getDepartamento().getId() != Catalogo.DEPARTAMENTO_1) {
      passwordEncoder.matches(clave, hashFicticio);
      throw new BadCredentialsException("fail");
    }
    if (!passwordEncoder.matches(clave, usuario.getPasswordHash())) {
      throw new BadCredentialsException("fail");
    }
    return new UsuarioPrincipal(
        usuario.getId(),
        usuario.getEmail(),
        usuario.getNombre(),
        usuario.getDepartamento().getId());
  }
}
