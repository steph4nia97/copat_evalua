package cl.copat.evalua.shared.aplicacion;

import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.SolicitudLog;
import cl.copat.evalua.departamento1.persistencia.SolicitudLogRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.shared.persistencia.RegistroActividad;
import cl.copat.evalua.shared.persistencia.RegistroActividadRepository;
import java.time.Clock;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

@Service
public class ActividadService {

  private final SolicitudLogRepository bitacora;
  private final RegistroActividadRepository registros;
  private final Clock reloj;

  public ActividadService(
      SolicitudLogRepository bitacora,
      RegistroActividadRepository registros,
      Clock reloj) {
    this.bitacora = bitacora;
    this.registros = registros;
    this.reloj = reloj;
  }

  public void anotar(Solicitud solicitud, Usuario usuario, String accion, String detalle) {
    ZonedDateTime ahora = ZonedDateTime.now(reloj);
    var fecha = ahora.toLocalDate();
    var hora = ahora.toLocalTime().withNano(0);
    if (solicitud != null) {
      bitacora.saveAndFlush(new SolicitudLog(solicitud, fecha, hora, usuario, accion));
    }
    String texto = detalle == null || detalle.isBlank() ? accion : detalle.trim();
    if (texto.length() > 300) {
      texto = texto.substring(0, 300);
    }
    registros.saveAndFlush(new RegistroActividad(
        fecha,
        hora,
        usuario,
        usuario.getDepartamento(),
        accion,
        texto,
        solicitud));
  }
}
