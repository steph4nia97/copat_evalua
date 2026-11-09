package cl.copat.evalua.departamento1.aplicacion;

import cl.copat.evalua.departamento1.api.BitacoraResponse;
import cl.copat.evalua.departamento1.api.CrearSolicitudRequest;
import cl.copat.evalua.departamento1.api.SolicitudResponse;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Correo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.NombreEmpresa;
import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.dominio.TelefonoChile;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitud;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Plantilla;
import cl.copat.evalua.departamento1.persistencia.PlantillaRepository;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.SolicitudLog;
import cl.copat.evalua.departamento1.persistencia.SolicitudLogRepository;
import cl.copat.evalua.departamento1.persistencia.SolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.shared.web.PeriodoDuplicadoException;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SolicitudService {

  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final SolicitudRepository solicitudes;
  private final SolicitudLogRepository bitacora;
  private final UsuarioRepository usuarios;
  private final PlantillaRepository plantillas;
  private final EstadoSolicitudRepository estados;
  private final Clock reloj;

  public SolicitudService(
      SolicitudRepository solicitudes,
      SolicitudLogRepository bitacora,
      UsuarioRepository usuarios,
      PlantillaRepository plantillas,
      EstadoSolicitudRepository estados,
      Clock reloj) {
    this.solicitudes = solicitudes;
    this.bitacora = bitacora;
    this.usuarios = usuarios;
    this.plantillas = plantillas;
    this.estados = estados;
    this.reloj = reloj;
  }

  public List<String> estados() {
    return estados.findAllByOrderByIdAsc().stream().map(EstadoSolicitud::getNombre).toList();
  }

  public List<String> plantillas() {
    return plantillas.findAllByOrderByIdAsc().stream().map(Plantilla::getNombre).toList();
  }

  public List<SolicitudResponse> listar(String estado, UsuarioPrincipal principal) {
    exigirDepartamento(principal);
    if (estado == null || estado.isBlank()) {
      return solicitudes.listarTodas(Catalogo.DEPARTAMENTO_1).stream().map(this::aRespuesta).toList();
    }
    String filtro = estado.trim();
    if (!Catalogo.ESTADOS.contains(filtro)) {
      throw new ReglaNegocioException(Mensajes.ESTADO);
    }
    return solicitudes.listarPorEstado(Catalogo.DEPARTAMENTO_1, filtro).stream().map(this::aRespuesta).toList();
  }

  public List<BitacoraResponse> bitacora(UsuarioPrincipal principal) {
    exigirDepartamento(principal);
    return bitacora.listar(Catalogo.DEPARTAMENTO_1).stream().map(this::aBitacora).toList();
  }

  @Transactional
  public SolicitudResponse crear(CrearSolicitudRequest request, UsuarioPrincipal principal) {
    exigirDepartamento(principal);
    validar(request);

    String nombre = NombreEmpresa.normalizar(request.nombreEmpresa());
    String rut = Rut.normalizar(request.rut());
    String correo = Correo.normalizar(request.correo());
    String telefono = TelefonoChile.normalizar(request.telefono());
    LocalDate inicio = Fechas.parse(request.periodoInicio().trim());
    LocalDate fin = Fechas.parse(request.periodoFin().trim());
    String descripcion = normalizarDescripcion(request.descripcion());

    if (solicitudes.existsByRutAndPeriodoInicioAndPeriodoFin(rut, inicio, fin)) {
      throw new PeriodoDuplicadoException();
    }

    Usuario usuario = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    Plantilla plantilla = plantillas.findByNombre(request.plantilla().trim())
        .orElseThrow(() -> new ReglaNegocioException(Mensajes.PLANTILLA, Map.of("plantilla", Mensajes.PLANTILLA)));
    EstadoSolicitud estado = estados.findByNombre(Catalogo.ESTADO_INICIAL)
        .orElseThrow(() -> new IllegalStateException("Falta el estado inicial"));

    Solicitud solicitud = new Solicitud(
        nombre,
        rut,
        correo,
        telefono,
        inicio,
        fin,
        plantilla,
        descripcion,
        estado,
        usuario.getDepartamento(),
        usuario);

    ZonedDateTime ahora = ZonedDateTime.now(reloj);
    try {
      solicitudes.saveAndFlush(solicitud);
      bitacora.saveAndFlush(new SolicitudLog(
          solicitud,
          ahora.toLocalDate(),
          ahora.toLocalTime().withNano(0),
          usuario,
          Catalogo.ACCION_CREADA));
    } catch (DataIntegrityViolationException ex) {
      if (esDuplicado(ex)) {
        throw new PeriodoDuplicadoException();
      }
      throw ex;
    }
    return aRespuesta(solicitud);
  }

  private void validar(CrearSolicitudRequest request) {
    Map<String, String> campos = new LinkedHashMap<>();
    if (request.nombreEmpresa() == null || request.nombreEmpresa().isBlank()) {
      campos.put("nombreEmpresa", Mensajes.NOMBRE_REQUERIDO);
    } else if (!NombreEmpresa.esValido(NombreEmpresa.normalizar(request.nombreEmpresa()))) {
      campos.put("nombreEmpresa", Mensajes.NOMBRE);
    }
    if (request.rut() == null || request.rut().isBlank()) {
      campos.put("rut", Mensajes.RUT_REQUERIDO);
    } else if (!Rut.esValido(request.rut())) {
      campos.put("rut", Mensajes.RUT);
    }
    if (request.correo() == null || request.correo().isBlank()) {
      campos.put("correo", Mensajes.CORREO_REQUERIDO);
    } else if (!Correo.esValido(Correo.normalizar(request.correo()))) {
      campos.put("correo", Mensajes.CORREO);
    }
    if (!TelefonoChile.esValido(request.telefono())) {
      campos.put("telefono", Mensajes.TELEFONO);
    }
    boolean inicioValido = request.periodoInicio() != null && Fechas.esValida(request.periodoInicio().trim());
    boolean finValido = request.periodoFin() != null && Fechas.esValida(request.periodoFin().trim());
    if (!inicioValido) {
      campos.put("periodoInicio", Mensajes.FECHA_INICIO);
    }
    if (!finValido) {
      campos.put("periodoFin", Mensajes.FECHA_FIN);
    }
    if (inicioValido && finValido) {
      LocalDate inicio = Fechas.parse(request.periodoInicio().trim());
      LocalDate fin = Fechas.parse(request.periodoFin().trim());
      if (fin.isBefore(inicio)) {
        campos.put("periodoFin", Mensajes.PERIODO);
      }
    }
    if (request.plantilla() == null || request.plantilla().isBlank()) {
      campos.put("plantilla", Mensajes.PLANTILLA_REQUERIDA);
    } else if (!Catalogo.PLANTILLAS.contains(request.plantilla().trim())) {
      campos.put("plantilla", Mensajes.PLANTILLA);
    }
    String descripcion = normalizarDescripcion(request.descripcion());
    if (descripcion != null && descripcion.length() > 300) {
      campos.put("descripcion", Mensajes.DESCRIPCION);
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
  }

  private static String normalizarDescripcion(String valor) {
    if (valor == null) {
      return null;
    }
    String limpia = valor.trim();
    return limpia.isEmpty() ? null : limpia;
  }

  private void exigirDepartamento(UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_1) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }

  private SolicitudResponse aRespuesta(Solicitud solicitud) {
    return new SolicitudResponse(
        solicitud.getId(),
        solicitud.getNombreEmpresa(),
        Rut.formatear(solicitud.getRut()),
        solicitud.getCorreo(),
        solicitud.getTelefono(),
        Fechas.format(solicitud.getPeriodoInicio()),
        Fechas.format(solicitud.getPeriodoFin()),
        solicitud.getPlantilla().getNombre(),
        solicitud.getDescripcion(),
        solicitud.getEstado().getNombre(),
        firmar(solicitud.getCreadoPor()));
  }

  private BitacoraResponse aBitacora(SolicitudLog log) {
    Solicitud solicitud = log.getSolicitud();
    return new BitacoraResponse(
        log.getId(),
        solicitud.getId(),
        solicitud.getNombreEmpresa(),
        Fechas.format(solicitud.getPeriodoInicio()),
        Fechas.format(solicitud.getPeriodoFin()),
        Fechas.format(log.getFecha()),
        log.getHora().format(HORA),
        firmar(log.getUsuario()),
        log.getAccion());
  }

  private static String firmar(Usuario usuario) {
    return usuario.getNombre() + " (" + usuario.getEmail() + ")";
  }

  private static boolean esDuplicado(DataIntegrityViolationException ex) {
    Throwable actual = ex;
    while (actual != null) {
      if (actual instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
        return true;
      }
      String mensaje = actual.getMessage();
      if (mensaje != null && mensaje.contains("uq_solicitud_rut_periodo")) {
        return true;
      }
      actual = actual.getCause();
    }
    return false;
  }
}
