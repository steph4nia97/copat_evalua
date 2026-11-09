package cl.copat.evalua.departamento1.persistencia;

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
}
