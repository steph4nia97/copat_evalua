package cl.copat.evalua.publico.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnlaceEncuestaRepository extends JpaRepository<EnlaceEncuesta, Long> {

  Optional<EnlaceEncuesta> findByToken(String token);
}
