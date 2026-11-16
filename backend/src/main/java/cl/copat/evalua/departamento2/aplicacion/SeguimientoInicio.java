package cl.copat.evalua.departamento2.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class SeguimientoInicio {

  private final ActividadService actividad;

  public SeguimientoInicio(ActividadService actividad) {
    this.actividad = actividad;
  }

  public void abrir(Solicitud solicitud, Usuario usuario, BigDecimal promedio, String desempeno) {
    boolean primera = solicitud.getSeguimiento() == null;
    if (primera) {
      solicitud.definirSeguimiento(Catalogo.SEGUIMIENTO_RECIBIDA);
    }
    actividad.anotar(solicitud, usuario, Catalogo.evaluacion(desempeno, promedio), solicitud.getNombreEmpresa());
    if (primera) {
      actividad.anotar(
          solicitud,
          usuario,
          Catalogo.seguimiento(Catalogo.SEGUIMIENTO_RECIBIDA),
          solicitud.getNombreEmpresa());
    }
  }
}
