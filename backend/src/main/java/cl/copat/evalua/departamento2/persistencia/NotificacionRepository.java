package cl.copat.evalua.departamento2.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

  boolean existsBySolicitud_IdAndDepartamento_Id(Long solicitudId, Short departamentoId);

  @Query("""
      select n from Notificacion n
      join fetch n.solicitud s
      join fetch s.plantilla
      join fetch s.estado
      join fetch s.creadoPor
      where n.departamento.id = :departamentoId
      order by n.id desc
      """)
  List<Notificacion> listar(@Param("departamentoId") short departamentoId);
}
