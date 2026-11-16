package cl.copat.evalua.departamento3.persistencia;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeguimientoNotaRepository extends JpaRepository<SeguimientoNota, Long> {

  @Query("""
      select n from SeguimientoNota n
      join fetch n.usuario
      join fetch n.solicitud
      where n.solicitud.id in :ids
      order by n.id asc
      """)
  List<SeguimientoNota> porSolicitudes(@Param("ids") Collection<Long> ids);
}
