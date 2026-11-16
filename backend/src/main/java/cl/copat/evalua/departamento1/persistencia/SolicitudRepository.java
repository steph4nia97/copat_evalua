package cl.copat.evalua.departamento1.persistencia;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

  boolean existsByRutAndPeriodoInicioAndPeriodoFin(String rut, LocalDate periodoInicio, LocalDate periodoFin);

  @Query("""
      select distinct s from Solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.departamento
      join fetch s.creadoPor
      left join fetch s.sedes
      where s.id in :ids
      """)
  List<Solicitud> porIds(@Param("ids") Collection<Long> ids);

  @Query("""
      select distinct s from Solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.departamento
      join fetch s.creadoPor
      left join fetch s.sedes
      where s.id = :id
      """)
  Optional<Solicitud> buscar(@Param("id") Long id);

  @Query("""
      select distinct s from Solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.creadoPor
      left join fetch s.sedes
      where s.departamento.id = :departamentoId
      order by s.id desc
      """)
  List<Solicitud> listarTodas(@Param("departamentoId") short departamentoId);

  @Query("""
      select distinct s from Solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.creadoPor
      left join fetch s.sedes
      where s.departamento.id = :departamentoId
        and s.estado.nombre = :estado
      order by s.id desc
      """)
  List<Solicitud> listarPorEstado(@Param("departamentoId") short departamentoId, @Param("estado") String estado);
}
