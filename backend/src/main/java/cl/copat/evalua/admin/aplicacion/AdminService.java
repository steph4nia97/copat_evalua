package cl.copat.evalua.admin.aplicacion;

import cl.copat.evalua.admin.api.CrearUsuarioRequest;
import cl.copat.evalua.admin.api.RegistroResponse;
import cl.copat.evalua.admin.api.UsuarioCreadoResponse;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Correo;
import cl.copat.evalua.departamento1.dominio.Fechas;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.persistencia.DepartamentoRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.shared.persistencia.RegistroActividadRepository;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminService {

  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final UsuarioRepository usuarios;
  private final DepartamentoRepository departamentos;
  private final RegistroActividadRepository registros;
  private final ActividadService actividad;
  private final PasswordEncoder passwordEncoder;

  public AdminService(
      UsuarioRepository usuarios,
      DepartamentoRepository departamentos,
      RegistroActividadRepository registros,
      ActividadService actividad,
      PasswordEncoder passwordEncoder) {
    this.usuarios = usuarios;
    this.departamentos = departamentos;
    this.registros = registros;
    this.actividad = actividad;
    this.passwordEncoder = passwordEncoder;
  }

  public List<UsuarioCreadoResponse> usuarios(UsuarioPrincipal principal) {
    exigir(principal);
    return usuarios.findAllByOrderByNombreAsc().stream().map(this::aUsuario).toList();
  }

  public List<RegistroResponse> registros(Short departamentoId, UsuarioPrincipal principal) {
    exigir(principal);
    return registros.listar().stream()
        .filter(registro -> departamentoId == null || registro.getDepartamento().getId().equals(departamentoId))
        .limit(300)
        .map(registro -> new RegistroResponse(
            registro.getId(),
            Fechas.format(registro.getFecha()),
            registro.getHora().format(HORA),
            registro.getUsuario().getNombre(),
            registro.getDepartamento().getNombre(),
            registro.getAccion(),
            registro.getDetalle()))
        .toList();
  }

  @Transactional
  public UsuarioCreadoResponse crear(CrearUsuarioRequest request, UsuarioPrincipal principal) {
    exigir(principal);
    String correo = Correo.normalizar(request == null ? null : request.correo()).toLowerCase(Locale.ROOT);
    String clave = request == null || request.clave() == null ? "" : request.clave();
    String nombre = request == null || request.nombre() == null ? "" : request.nombre().trim();
    Short departamentoId = request == null ? null : request.departamentoId();
    Map<String, String> campos = new LinkedHashMap<>();
    if (!Correo.esValido(correo)) {
      campos.put("correo", Mensajes.CORREO);
    } else if (usuarios.findByEmailIgnoreCase(correo).isPresent()) {
      campos.put("correo", Mensajes.CORREO_REGISTRADO);
    }
    if (clave.length() < 8 || clave.length() > 72) {
      campos.put("clave", Mensajes.CLAVE_CORTA);
    }
    if (nombre.isEmpty() || nombre.length() > 80) {
      campos.put("nombre", Mensajes.NOMBRE_USUARIO);
    }
    if (departamentoId == null
        || (departamentoId != Catalogo.DEPARTAMENTO_1
            && departamentoId != Catalogo.DEPARTAMENTO_3)) {
      campos.put("departamentoId", Mensajes.DEPARTAMENTO_USUARIO);
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    Usuario admin = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    Usuario creado = new Usuario(
        correo,
        passwordEncoder.encode(clave),
        nombre,
        departamentos.getReferenceById(departamentoId),
        true);
    try {
      usuarios.saveAndFlush(creado);
    } catch (DataIntegrityViolationException ex) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("correo", Mensajes.CORREO_REGISTRADO));
    }
    actividad.anotar(
        null,
        admin,
        Catalogo.ACCION_USUARIO_CREADO,
        nombre + " (" + correo + ") en " + Catalogo.nombreDepartamento(departamentoId));
    return aUsuario(creado);
  }

  private UsuarioCreadoResponse aUsuario(Usuario usuario) {
    short departamentoId = usuario.getDepartamento().getId();
    return new UsuarioCreadoResponse(
        usuario.getId(),
        usuario.getEmail(),
        usuario.getNombre(),
        Catalogo.nombreDepartamento(departamentoId),
        departamentoId,
        usuario.isClaveProvisoria());
  }

  private void exigir(UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }
}
