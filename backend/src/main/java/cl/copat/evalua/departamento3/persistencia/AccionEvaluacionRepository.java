package cl.copat.evalua.departamento3.persistencia;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccionEvaluacionRepository extends JpaRepository<AccionEvaluacion, Long> {

  @Query("""
      select a from AccionEvaluacion a
      where a.solicitud.id in :ids
      order by a.id
      """)
  List<AccionEvaluacion> porSolicitudes(@Param("ids") Collection<Long> ids);
}
