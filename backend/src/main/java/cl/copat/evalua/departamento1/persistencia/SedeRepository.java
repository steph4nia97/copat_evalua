package cl.copat.evalua.departamento1.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SedeRepository extends JpaRepository<Sede, Short> {

  List<Sede> findAllByOrderByIdAsc();

  List<Sede> findByNombreIn(Collection<String> nombres);

  Optional<Sede> findByNombre(String nombre);
}
