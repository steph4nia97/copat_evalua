package cl.copat.evalua.departamento1.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByEmailIgnoreCase(String email);
}
