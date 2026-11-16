package cl.copat.evalua.departamento3.persistencia;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenciaRevisionRepository extends JpaRepository<EvidenciaRevision, Long> {

  @Query("select e.solicitudId from EvidenciaRevision e where e.solicitudId in :ids")
  List<Long> identificadoresPresentes(@Param("ids") Collection<Long> ids);
}
