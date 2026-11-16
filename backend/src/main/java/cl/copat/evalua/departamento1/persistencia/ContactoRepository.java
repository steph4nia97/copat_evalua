package cl.copat.evalua.departamento1.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {

  List<Contacto> findByRutOrderByIdAsc(String rut);

  List<Contacto> findAllByOrderByRutAscIdAsc();
}
