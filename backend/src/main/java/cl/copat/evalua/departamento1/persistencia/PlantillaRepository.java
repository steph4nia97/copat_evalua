package cl.copat.evalua.departamento1.persistencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaRepository extends JpaRepository<Plantilla, Short> {

  List<Plantilla> findAllByOrderByIdAsc();

  Optional<Plantilla> findByNombre(String nombre);
}
