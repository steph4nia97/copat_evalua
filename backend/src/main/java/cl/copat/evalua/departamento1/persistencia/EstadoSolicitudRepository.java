package cl.copat.evalua.departamento1.persistencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoSolicitudRepository extends JpaRepository<EstadoSolicitud, Short> {

  List<EstadoSolicitud> findAllByOrderByIdAsc();

  Optional<EstadoSolicitud> findByNombre(String nombre);
}
