package cl.copat.evalua.departamento1.seguridad;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UsuarioPrincipal(Long id, String email, String nombre, short departamentoId)
    implements UserDetails, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_DEPARTAMENTO_" + departamentoId));
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return email;
  }
}
