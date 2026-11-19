package cl.copat.evalua.publico.persistencia;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InformeGestionRepository extends JpaRepository<InformeGestion, Long> {

  Optional<InformeGestion> findByPeriodoInicioAndPeriodoFin(LocalDate periodoInicio, LocalDate periodoFin);
}
