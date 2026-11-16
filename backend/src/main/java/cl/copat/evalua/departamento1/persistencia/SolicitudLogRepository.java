package cl.copat.evalua.departamento1.persistencia;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudLogRepository extends JpaRepository<SolicitudLog, Long> {

  @Query("""
      select l from SolicitudLog l
      join fetch l.solicitud s
      join fetch l.usuario
      where s.departamento.id = :departamentoId
      order by l.id desc
      """)
  List<SolicitudLog> listar(@Param("departamentoId") short departamentoId);

  @Query("""
      select l from SolicitudLog l
      join fetch l.usuario
      join fetch l.solicitud
      where l.solicitud.id in :ids
      order by l.id asc
      """)
  List<SolicitudLog> porSolicitudes(@Param("ids") Collection<Long> ids);

  @Query("""
      select l from SolicitudLog l
      join fetch l.usuario
      where l.solicitud.id = :id and l.accion in :acciones
      order by l.id desc
      """)
  List<SolicitudLog> porAcciones(@Param("id") Long id, @Param("acciones") Collection<String> acciones);
}
