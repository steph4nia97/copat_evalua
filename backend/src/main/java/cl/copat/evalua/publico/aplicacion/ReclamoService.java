package cl.copat.evalua.publico.aplicacion;

import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Correo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.TelefonoChile;
import cl.copat.evalua.departamento1.persistencia.Sede;
import cl.copat.evalua.departamento1.persistencia.SedeRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.publico.api.ReclamoRequest;
import cl.copat.evalua.publico.api.ReclamoResponse;
import cl.copat.evalua.publico.api.SeguimientoRequest;
import cl.copat.evalua.publico.dominio.ReclamoCatalogo;
import cl.copat.evalua.publico.persistencia.Reclamo;
import cl.copat.evalua.publico.persistencia.ReclamoRepository;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ReclamoService {

  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final ReclamoRepository reclamos;
  private final SedeRepository sedes;
  private final Clock reloj;

  public ReclamoService(ReclamoRepository reclamos, SedeRepository sedes, Clock reloj) {
    this.reclamos = reclamos;
    this.sedes = sedes;
    this.reloj = reloj;
  }

  @Transactional
  public long recibir(ReclamoRequest request) {
    if (request == null) {
      throw new ReglaNegocioException(Mensajes.CUERPO);
    }
    Map<String, String> campos = new LinkedHashMap<>();
    String nombre = texto(request.nombre());
    String telefono = digitos(request.telefono());
    String obra = texto(request.obra());
    String fecha = texto(request.fechaServicio());
    String empresa = texto(request.empresa());
    String correo = Correo.normalizar(request.correo());
    String ubicacion = texto(request.ubicacion());
    String guia = texto(request.guiaDespacho());
    String tipo = texto(request.tipo());
    String descripcion = texto(request.descripcion());
    String preferencia = texto(request.preferenciaContacto());
    String horario = texto(request.horario());
    String comentarios = texto(request.comentarios());
    if (nombre.isEmpty() || nombre.length() > 120) {
      campos.put("nombre", Mensajes.RECLAMO_NOMBRE);
    }
    if (telefono.isEmpty() || !TelefonoChile.esValido(telefono)) {
      campos.put("telefono", Mensajes.RECLAMO_TELEFONO);
    }
    if (obra.isEmpty() || obra.length() > 200) {
      campos.put("obra", Mensajes.RECLAMO_OBRA);
    }
    if (!Fechas.esValida(fecha)) {
      campos.put("fechaServicio", Mensajes.RECLAMO_FECHA);
    }
    if (empresa.isEmpty() || empresa.length() > 150) {
      campos.put("empresa", Mensajes.RECLAMO_EMPRESA);
    }
    if (!Correo.esValido(correo)) {
      campos.put("correo", Mensajes.CORREO);
    }
    if (ubicacion.isEmpty() || ubicacion.length() > 200) {
      campos.put("ubicacion", Mensajes.RECLAMO_UBICACION);
    }
    if (guia.length() > 40) {
      campos.put("guiaDespacho", Mensajes.RECLAMO_GUIA);
    }
    if (!ReclamoCatalogo.TIPOS.contains(tipo)) {
      campos.put("tipo", Mensajes.RECLAMO_TIPO);
    }
    if (descripcion.isEmpty() || descripcion.length() > 1000) {
      campos.put("descripcion", Mensajes.RECLAMO_DESCRIPCION);
    }
    if (!ReclamoCatalogo.PREFERENCIAS.contains(preferencia)) {
      campos.put("preferenciaContacto", Mensajes.RECLAMO_PREFERENCIA);
    }
    if (!horario.isEmpty() && !ReclamoCatalogo.HORARIOS.contains(horario)) {
      campos.put("horario", Mensajes.RECLAMO_HORARIO);
    }
    if (comentarios.length() > 500) {
      campos.put("comentarios", Mensajes.RECLAMO_COMENTARIO);
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    Reclamo reclamo = reclamos.saveAndFlush(new Reclamo(
        nombre,
        telefono,
        obra,
        Fechas.parse(fecha),
        empresa,
        correo,
        ubicacion,
        guia.isEmpty() ? null : guia,
        tipo,
        descripcion,
        preferencia,
        horario.isEmpty() ? null : horario,
        comentarios.isEmpty() ? null : comentarios,
        LocalDateTime.now(reloj)));
    return reclamo.getId();
  }

  public List<ReclamoResponse> listar(UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_3) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
    return reclamos.findAllByOrderByIdDesc().stream().map(ReclamoService::respuesta).toList();
  }

  private static ReclamoResponse respuesta(Reclamo reclamo) {
    return new ReclamoResponse(
        reclamo.getId(),
        Fechas.format(reclamo.getRecibidoEn().toLocalDate()),
        reclamo.getRecibidoEn().toLocalTime().withNano(0).format(HORA),
        reclamo.getNombre(),
        reclamo.getTelefono(),
        reclamo.getEmpresa(),
        reclamo.getCorreo(),
        reclamo.getObra(),
        Fechas.format(reclamo.getFechaServicio()),
        reclamo.getUbicacion(),
        reclamo.getGuiaDespacho(),
        reclamo.getTipo(),
        reclamo.getDescripcion(),
        reclamo.getPreferenciaContacto(),
        reclamo.getHorario(),
        reclamo.getComentarios(),
        reclamo.getEstado(),
        reclamo.getAreaResponsable(),
        reclamo.getResponsable(),
        reclamo.getFechaGestion() == null ? null : Fechas.format(reclamo.getFechaGestion()),
        reclamo.getObservaciones(),
        reclamo.getHoraCierre() == null ? null : reclamo.getHoraCierre().format(HORA),
        reclamo.getFechaCierre() == null ? null : Fechas.format(reclamo.getFechaCierre()),
        reclamo.getSolucion(),
        reclamo.getSede() == null ? null : reclamo.getSede().getNombre());
  }

  @Transactional
  public ReclamoResponse seguir(long id, SeguimientoRequest request, UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_3) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
    Reclamo reclamo = reclamos.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (request == null) {
      throw new ReglaNegocioException(Mensajes.CUERPO);
    }
    Map<String, String> campos = new LinkedHashMap<>();
    String estado = texto(request.estado());
    String area = texto(request.area());
    String responsable = texto(request.responsable());
    String gestion = texto(request.fechaGestion());
    String observaciones = texto(request.observaciones());
    String cierre = texto(request.fechaCierre());
    String hora = texto(request.horaCierre());
    String solucion = texto(request.solucion());
    String sedeNombre = texto(request.sede());
    boolean gestiona = ReclamoCatalogo.ESTADO_GESTION.equals(estado) || ReclamoCatalogo.ESTADO_CERRADO.equals(estado);
    boolean cierra = ReclamoCatalogo.ESTADO_CERRADO.equals(estado);
    if (!ReclamoCatalogo.ESTADOS.contains(estado)) {
      campos.put("estado", Mensajes.RECLAMO_SEGUIMIENTO);
    }
    if (gestiona && (area.isEmpty() || area.length() > 80)) {
      campos.put("area", Mensajes.RECLAMO_AREA);
    } else if (area.length() > 80) {
      campos.put("area", Mensajes.RECLAMO_AREA);
    }
    if (gestiona && (responsable.isEmpty() || responsable.length() > 80)) {
      campos.put("responsable", Mensajes.RECLAMO_RESPONSABLE);
    } else if (responsable.length() > 80) {
      campos.put("responsable", Mensajes.RECLAMO_RESPONSABLE);
    }
    if (gestiona && !Fechas.esValida(gestion)) {
      campos.put("fechaGestion", Mensajes.RECLAMO_GESTION);
    } else if (!gestion.isEmpty() && !Fechas.esValida(gestion)) {
      campos.put("fechaGestion", Mensajes.RECLAMO_GESTION);
    }
    if (observaciones.length() > 500) {
      campos.put("observaciones", Mensajes.RECLAMO_OBS);
    }
    if (cierra && (solucion.isEmpty() || solucion.length() > 1000)) {
      campos.put("solucion", Mensajes.RECLAMO_SOLUCION);
    } else if (solucion.length() > 1000) {
      campos.put("solucion", Mensajes.RECLAMO_SOLUCION);
    }
    if (!cierre.isEmpty() && !Fechas.esValida(cierre)) {
      campos.put("fechaCierre", Mensajes.RECLAMO_CIERRE);
    }
    LocalTime horaCierre = null;
    if (!hora.isEmpty()) {
      try {
        horaCierre = LocalTime.parse(hora, HORA);
      } catch (DateTimeParseException ex) {
        campos.put("horaCierre", Mensajes.RECLAMO_HORA);
      }
    }
    Sede sede = null;
    if (gestiona && sedeNombre.isEmpty()) {
      campos.put("sede", Mensajes.RECLAMO_SEDE);
    } else if (!sedeNombre.isEmpty()) {
      sede = sedes.findByNombre(sedeNombre).orElse(null);
      if (sede == null) {
        campos.put("sede", Mensajes.RECLAMO_SEDE);
      }
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    LocalDateTime ahora = LocalDateTime.now(reloj);
    LocalDate fechaCierre = cierre.isEmpty() ? null : Fechas.parse(cierre);
    if (cierra && fechaCierre == null) {
      fechaCierre = ahora.toLocalDate();
    }
    if (cierra && horaCierre == null) {
      horaCierre = ahora.toLocalTime().withNano(0);
    }
    reclamo.registrarSeguimiento(
        estado,
        area.isEmpty() ? null : area,
        responsable.isEmpty() ? null : responsable,
        gestion.isEmpty() ? null : Fechas.parse(gestion),
        observaciones.isEmpty() ? null : observaciones,
        fechaCierre,
        horaCierre,
        solucion.isEmpty() ? null : solucion,
        sede);
    return respuesta(reclamo);
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor.trim();
  }

  private static String digitos(String valor) {
    return texto(valor).replaceAll("[^0-9]", "");
  }
}
