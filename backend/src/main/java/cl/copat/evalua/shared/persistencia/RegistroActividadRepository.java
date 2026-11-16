package cl.copat.evalua.shared.persistencia;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroActividadRepository extends JpaRepository<RegistroActividad, Long> {

  @Query("""
      select r from RegistroActividad r
      join fetch r.usuario
      join fetch r.departamento
      order by r.id desc
      """)
  List<RegistroActividad> listar();

  @Query("""
      select r from RegistroActividad r
      join fetch r.usuario
      where r.accion in :acciones
      order by r.id desc
      """)
  List<RegistroActividad> porAcciones(@Param("acciones") Collection<String> acciones);
}
