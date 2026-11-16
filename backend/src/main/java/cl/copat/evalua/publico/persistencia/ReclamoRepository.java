package cl.copat.evalua.publico.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {

  List<Reclamo> findAllByOrderByIdDesc();
}
