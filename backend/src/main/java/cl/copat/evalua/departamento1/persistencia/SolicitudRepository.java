package cl.copat.evalua.departamento1.persistencia;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

  boolean existsByRutAndPeriodoInicioAndPeriodoFin(String rut, LocalDate periodoInicio, LocalDate periodoFin);

  @Query("""
      select s from Solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.creadoPor
      where s.departamento.id = :departamentoId
      order by s.id desc
      """)
  List<Solicitud> listarTodas(@Param("departamentoId") short departamentoId);

  @Query("""
      select s from Solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.creadoPor
      where s.departamento.id = :departamentoId
        and s.estado.nombre = :estado
      order by s.id desc
      """)
  List<Solicitud> listarPorEstado(@Param("departamentoId") short departamentoId, @Param("estado") String estado);
}
