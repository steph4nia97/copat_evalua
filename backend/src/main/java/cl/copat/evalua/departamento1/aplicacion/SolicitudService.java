package cl.copat.evalua.departamento1.aplicacion;

import cl.copat.evalua.departamento1.api.BitacoraResponse;
import cl.copat.evalua.departamento1.api.CrearSolicitudRequest;
import cl.copat.evalua.departamento1.api.SolicitudResponse;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Correo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.NoAplica;
import cl.copat.evalua.departamento1.dominio.NombreEmpresa;
import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.dominio.TelefonoChile;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitud;
import cl.copat.evalua.departamento1.persistencia.EstadoSolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Plantilla;
import cl.copat.evalua.departamento1.persistencia.PlantillaRepository;
import cl.copat.evalua.departamento1.persistencia.Sede;
import cl.copat.evalua.departamento1.persistencia.SedeRepository;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.departamento1.persistencia.SolicitudLog;
import cl.copat.evalua.departamento1.persistencia.SolicitudLogRepository;
import cl.copat.evalua.departamento1.persistencia.SolicitudRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.shared.persistencia.RegistroActividad;
import cl.copat.evalua.shared.persistencia.RegistroActividadRepository;
import cl.copat.evalua.shared.web.PeriodoDuplicadoException;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final SedeRepository sedes;
  private final EstadoSolicitudRepository estados;
  private final ActividadService actividad;
  private final RegistroActividadRepository registros;
  private final Clock reloj;

  public SolicitudService(
      SolicitudRepository solicitudes,
      SolicitudLogRepository bitacora,
      UsuarioRepository usuarios,
      PlantillaRepository plantillas,
      SedeRepository sedes,
      EstadoSolicitudRepository estados,
      ActividadService actividad,
      RegistroActividadRepository registros,
      Clock reloj) {
    this.solicitudes = solicitudes;
    this.bitacora = bitacora;
    this.usuarios = usuarios;
    this.plantillas = plantillas;
    this.sedes = sedes;
    this.estados = estados;
    this.actividad = actividad;
    this.registros = registros;
    this.reloj = reloj;
  }

  public List<String> estados() {
    return estados.findAllByOrderByIdAsc().stream().map(EstadoSolicitud::getNombre).toList();
  }

  public List<String> plantillas() {
    return plantillas.findAllByOrderByIdAsc().stream().map(Plantilla::getNombre).toList();
  }

  public List<String> sedes() {
    return sedes.findAllByOrderByIdAsc().stream().map(Sede::getNombre).toList();
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
    return movimientos(true);
  }

  public List<BitacoraResponse> bitacoraSeguimiento(UsuarioPrincipal principal) {
    short departamento = principal.departamentoId();
    if (!Catalogo.operaSolicitudes(departamento) && departamento != Catalogo.DEPARTAMENTO_3) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
    return movimientos(Catalogo.operaSolicitudes(departamento));
  }

  private List<BitacoraResponse> movimientos(boolean operacion) {
    List<Fila> filas = new ArrayList<>();
    for (SolicitudLog log : bitacora.listar(Catalogo.DEPARTAMENTO_1)) {
      String accion = Catalogo.etiquetaBitacora(log.getAccion(), operacion);
      if (accion == null) {
        continue;
      }
      filas.add(new Fila(log.getFecha(), log.getHora(), log.getId(), aBitacora(log, accion)));
    }
    for (RegistroActividad registro : registros.porAcciones(
        List.of(Catalogo.ACCION_CARGA_CLIENTES, Catalogo.ACCION_CARGA_CONTACTOS))) {
      String detalle = registro.getDetalle();
      String accion = registro.getAccion();
      if (detalle != null && !detalle.isBlank() && !detalle.equals(accion)) {
        accion = accion + " · " + detalle;
      }
      filas.add(new Fila(
          registro.getFecha(),
          registro.getHora(),
          -registro.getId(),
          new BitacoraResponse(
              -registro.getId(),
              0,
              "—",
              "—",
              "—",
              Fechas.format(registro.getFecha()),
              registro.getHora().format(HORA),
              firmar(registro.getUsuario()),
              accion)));
    }
    filas.sort(Comparator.comparing(Fila::fecha).thenComparing(Fila::hora).thenComparing(Fila::orden).reversed());
    return filas.stream().map(Fila::respuesta).toList();
  }

  private record Fila(LocalDate fecha, LocalTime hora, long orden, BitacoraResponse respuesta) {}

  @Transactional
  public SolicitudResponse crear(CrearSolicitudRequest request, UsuarioPrincipal principal) {
    exigirDepartamento(principal);
    validar(request);

    String nombre = NombreEmpresa.normalizar(request.nombreEmpresa());
    String rut = Rut.normalizar(request.rut());
    String correo = correoDe(request.correo());
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
    List<String> nombresSede = normalizarSedes(request.sedes());
    List<Sede> encontradas = sedes.findByNombreIn(nombresSede);
    if (encontradas.size() != nombresSede.size()) {
      throw new ReglaNegocioException(Mensajes.SEDE, Map.of("sedes", Mensajes.SEDE));
    }
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
    solicitud.asignarSedes(encontradas);

    try {
      solicitudes.saveAndFlush(solicitud);
      actividad.anotar(solicitud, usuario, Catalogo.ACCION_CREADA, solicitud.getNombreEmpresa());
    } catch (DataIntegrityViolationException ex) {
      if (esDuplicado(ex)) {
        throw new PeriodoDuplicadoException();
      }
      throw ex;
    }
    return aRespuesta(solicitud);
  }

  @Transactional
  public SolicitudResponse editar(long id, CrearSolicitudRequest request, UsuarioPrincipal principal) {
    exigirDepartamento(principal);
    validar(request);
    Solicitud solicitud = solicitudes.buscar(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (!Catalogo.operaSolicitudes(solicitud.getDepartamento().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO);
    }
    if (!Catalogo.ESTADO_INICIAL.equals(solicitud.getEstado().getNombre())) {
      throw new ReglaNegocioException(Mensajes.EDICION_INGRESADA);
    }

    String nombre = NombreEmpresa.normalizar(request.nombreEmpresa());
    String rut = Rut.normalizar(request.rut());
    String correo = correoDe(request.correo());
    String telefono = TelefonoChile.normalizar(request.telefono());
    LocalDate inicio = Fechas.parse(request.periodoInicio().trim());
    LocalDate fin = Fechas.parse(request.periodoFin().trim());
    String descripcion = normalizarDescripcion(request.descripcion());
    if (solicitudes.existsByRutAndPeriodoInicioAndPeriodoFinAndIdNot(rut, inicio, fin, id)) {
      throw new PeriodoDuplicadoException();
    }
    Plantilla plantilla = plantillas.findByNombre(request.plantilla().trim())
        .orElseThrow(() -> new ReglaNegocioException(Mensajes.PLANTILLA, Map.of("plantilla", Mensajes.PLANTILLA)));
    List<String> nombresSede = normalizarSedes(request.sedes());
    List<Sede> encontradas = sedes.findByNombreIn(nombresSede);
    if (encontradas.size() != nombresSede.size()) {
      throw new ReglaNegocioException(Mensajes.SEDE, Map.of("sedes", Mensajes.SEDE));
    }
    solicitud.editar(nombre, rut, correo, telefono, inicio, fin, plantilla, descripcion);
    solicitud.asignarSedes(encontradas);
    try {
      solicitudes.saveAndFlush(solicitud);
    } catch (DataIntegrityViolationException ex) {
      if (esDuplicado(ex)) {
        throw new PeriodoDuplicadoException();
      }
      throw ex;
    }
    return aRespuesta(solicitud);
  }

  private static String correoDe(String valor) {
    if (NoAplica.es(valor)) {
      return NoAplica.TEXTO;
    }
    return Correo.normalizar(valor);
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
    } else if (!NoAplica.es(request.correo()) && !Correo.esValido(Correo.normalizar(request.correo()))) {
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
    List<String> nombresSede = normalizarSedes(request.sedes());
    if (nombresSede.isEmpty()) {
      campos.put("sedes", Mensajes.SEDE_REQUERIDA);
    } else if (!new LinkedHashSet<>(Catalogo.SEDES).containsAll(nombresSede)) {
      campos.put("sedes", Mensajes.SEDE);
    }
    String descripcion = normalizarDescripcion(request.descripcion());
    if (descripcion != null && descripcion.length() > 300) {
      campos.put("descripcion", Mensajes.DESCRIPCION);
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
  }

  private static List<String> normalizarSedes(List<String> sedes) {
    if (sedes == null) {
      return List.of();
    }
    Set<String> unicas = new LinkedHashSet<>();
    for (String sede : sedes) {
      if (sede == null) {
        continue;
      }
      String limpia = sede.trim();
      if (!limpia.isEmpty()) {
        unicas.add(limpia);
      }
    }
    return List.copyOf(unicas);
  }

  private static String normalizarDescripcion(String valor) {
    if (valor == null) {
      return null;
    }
    String limpia = valor.trim();
    return limpia.isEmpty() ? null : limpia;
  }

  private void exigirDepartamento(UsuarioPrincipal principal) {
    if (!Catalogo.operaSolicitudes(principal.departamentoId())) {
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
        solicitud.nombresSedes(),
        solicitud.getDescripcion(),
        solicitud.getEstado().getNombre(),
        firmar(solicitud.getCreadoPor()),
        null,
        null,
        false,
        null,
        null,
        Catalogo.vistaOperacion(solicitud.getSeguimiento()));
  }

  private BitacoraResponse aBitacora(SolicitudLog log, String accion) {
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
        accion);
  }

  private static String firmar(Usuario usuario) {
    return usuario.nombreVisible() + " (" + usuario.getEmail() + ")";
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
