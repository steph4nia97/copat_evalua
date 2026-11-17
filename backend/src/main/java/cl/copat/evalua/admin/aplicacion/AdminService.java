package cl.copat.evalua.admin.aplicacion;

import cl.copat.evalua.admin.api.AsignarCargoRequest;
import cl.copat.evalua.admin.api.CrearUsuarioRequest;
import cl.copat.evalua.admin.api.EditarUsuarioRequest;
import cl.copat.evalua.admin.api.RegistroResponse;
import cl.copat.evalua.admin.api.UsuarioCreadoResponse;
import cl.copat.evalua.departamento1.dominio.Cargo;
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
import jakarta.persistence.EntityManager;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
  private final EntityManager entityManager;

  public AdminService(
      UsuarioRepository usuarios,
      DepartamentoRepository departamentos,
      RegistroActividadRepository registros,
      ActividadService actividad,
      PasswordEncoder passwordEncoder,
      EntityManager entityManager) {
    this.usuarios = usuarios;
    this.departamentos = departamentos;
    this.registros = registros;
    this.actividad = actividad;
    this.passwordEncoder = passwordEncoder;
    this.entityManager = entityManager;
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
            registro.getUsuario().nombreVisible(),
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
    String cargo = Cargo.normalizar(request == null ? null : request.cargo());
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
    if (!Cargo.esValido(cargo)) {
      campos.put("cargo", Mensajes.CARGO);
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
        cargo,
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

  @Transactional
  public UsuarioCreadoResponse asignarCargo(long id, AsignarCargoRequest request, UsuarioPrincipal principal) {
    exigir(principal);
    String cargo = Cargo.normalizar(request == null ? null : request.cargo());
    if (!Cargo.esValido(cargo)) {
      throw new ReglaNegocioException(Mensajes.REVISE, Map.of("cargo", Mensajes.CARGO));
    }
    Usuario usuario = usuarios.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    Usuario admin = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    String anterior = usuario.getCargo();
    if (!anterior.equals(cargo)) {
      usuario.asignarCargo(cargo);
      usuarios.saveAndFlush(usuario);
      actividad.anotar(null, admin, Catalogo.ACCION_CARGO, usuario.getNombre() + ": " + anterior + " ahora es " + cargo);
    }
    return aUsuario(usuario);
  }

  @Transactional
  public UsuarioCreadoResponse editar(long id, EditarUsuarioRequest request, UsuarioPrincipal principal) {
    exigir(principal);
    Usuario usuario = usuarios.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    boolean esAdmin = usuario.getDepartamento().getId() == Catalogo.DEPARTAMENTO_ADMIN;
    String correo = Correo.normalizar(request == null ? null : request.correo()).toLowerCase(Locale.ROOT);
    String nombre = request == null || request.nombre() == null ? "" : request.nombre().trim();
    String cargo = Cargo.normalizar(request == null ? null : request.cargo());
    String clave = request == null || request.clave() == null ? "" : request.clave();
    Short departamentoId = request == null ? null : request.departamentoId();
    Map<String, String> campos = new LinkedHashMap<>();
    if (!Correo.esValido(correo)) {
      campos.put("correo", Mensajes.CORREO);
    } else if (!correo.equalsIgnoreCase(usuario.getEmail()) && usuarios.findByEmailIgnoreCase(correo).isPresent()) {
      campos.put("correo", Mensajes.CORREO_REGISTRADO);
    }
    if (nombre.isEmpty() || nombre.length() > 80) {
      campos.put("nombre", Mensajes.NOMBRE_USUARIO);
    }
    if (!Cargo.esValido(cargo)) {
      campos.put("cargo", Mensajes.CARGO);
    }
    if (!clave.isEmpty() && (clave.length() < 8 || clave.length() > 72)) {
      campos.put("clave", Mensajes.CLAVE_CORTA);
    }
    if (esAdmin) {
      if (departamentoId != null && departamentoId != Catalogo.DEPARTAMENTO_ADMIN) {
        campos.put("departamentoId", Mensajes.DEPARTAMENTO_ADMIN);
      }
    } else if (departamentoId == null
        || (departamentoId != Catalogo.DEPARTAMENTO_1 && departamentoId != Catalogo.DEPARTAMENTO_3)) {
      campos.put("departamentoId", Mensajes.DEPARTAMENTO_USUARIO);
    }
    if (!campos.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.REVISE, campos);
    }
    List<String> cambios = new ArrayList<>();
    if (!usuario.getEmail().equalsIgnoreCase(correo)) {
      usuario.asignarCorreo(correo);
      cambios.add("correo");
    }
    if (!usuario.getNombre().equals(nombre)) {
      usuario.renombrar(nombre);
      cambios.add("nombre");
    }
    if (!usuario.getCargo().equals(cargo)) {
      usuario.asignarCargo(cargo);
      cambios.add("cargo");
    }
    if (!esAdmin && !usuario.getDepartamento().getId().equals(departamentoId)) {
      usuario.asignarDepartamento(departamentos.getReferenceById(departamentoId));
      cambios.add("departamento");
    }
    if (!clave.isEmpty()) {
      usuario.asignarClaveProvisoria(passwordEncoder.encode(clave));
      cambios.add("clave");
    }
    if (!cambios.isEmpty()) {
      try {
        usuarios.saveAndFlush(usuario);
      } catch (DataIntegrityViolationException ex) {
        throw new ReglaNegocioException(Mensajes.REVISE, Map.of("correo", Mensajes.CORREO_REGISTRADO));
      }
      Usuario admin = usuarios.findById(principal.id())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
      actividad.anotar(null, admin, Catalogo.ACCION_USUARIO_ACTUALIZADO, usuario.getNombre() + ": " + String.join(", ", cambios));
    }
    return aUsuario(usuario);
  }

  @Transactional
  public void eliminar(long id, UsuarioPrincipal principal) {
    exigir(principal);
    if (principal.id() == id) {
      throw new ReglaNegocioException(Mensajes.USUARIO_PROPIO);
    }
    Usuario usuario = usuarios.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (tieneMovimientos(id)) {
      throw new ReglaNegocioException(Mensajes.USUARIO_CON_MOVIMIENTOS);
    }
    String detalle = usuario.getNombre() + " (" + usuario.getEmail() + ")";
    usuarios.delete(usuario);
    usuarios.flush();
    Usuario admin = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    actividad.anotar(null, admin, Catalogo.ACCION_USUARIO_ELIMINADO, detalle);
  }

  @Transactional
  public UsuarioCreadoResponse desactivar(long id, UsuarioPrincipal principal) {
    return cambiarAcceso(id, principal, false);
  }

  @Transactional
  public UsuarioCreadoResponse reactivar(long id, UsuarioPrincipal principal) {
    return cambiarAcceso(id, principal, true);
  }

  private UsuarioCreadoResponse cambiarAcceso(long id, UsuarioPrincipal principal, boolean activo) {
    exigir(principal);
    if (principal.id() == id) {
      throw new ReglaNegocioException(Mensajes.USUARIO_PROPIO);
    }
    Usuario usuario = usuarios.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Mensajes.NO_ENCONTRADO));
    if (usuario.isActivo() != activo) {
      if (activo) {
        usuario.reactivar();
      } else {
        usuario.desactivar();
      }
      usuarios.saveAndFlush(usuario);
      Usuario admin = usuarios.findById(principal.id())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
      actividad.anotar(
          null,
          admin,
          activo ? Catalogo.ACCION_USUARIO_REACTIVADO : Catalogo.ACCION_USUARIO_DESACTIVADO,
          usuario.getNombre() + " (" + usuario.getEmail() + ")");
    }
    return aUsuario(usuario);
  }

  private boolean tieneMovimientos(long id) {
    Number total = (Number) entityManager.createNativeQuery("""
        SELECT
          (SELECT count(*) FROM solicitud WHERE creado_por_id = ?1)
          + (SELECT count(*) FROM solicitud_log WHERE usuario_id = ?1)
          + (SELECT count(*) FROM revision_departamento3 WHERE usuario_id = ?1)
          + (SELECT count(*) FROM rechazo_solicitud WHERE rechazado_por_id = ?1 OR destinatario_id = ?1)
          + (SELECT count(*) FROM registro_actividad WHERE usuario_id = ?1)
          + (SELECT count(*) FROM seguimiento_nota WHERE usuario_id = ?1)
        """)
        .setParameter(1, id)
        .getSingleResult();
    return total.longValue() > 0;
  }

  private UsuarioCreadoResponse aUsuario(Usuario usuario) {
    short departamentoId = usuario.getDepartamento().getId();
    return new UsuarioCreadoResponse(
        usuario.getId(),
        usuario.getEmail(),
        usuario.getNombre(),
        usuario.getCargo(),
        Catalogo.nombreDepartamento(departamentoId),
        departamentoId,
        usuario.isClaveProvisoria(),
        usuario.isActivo());
  }

  private void exigir(UsuarioPrincipal principal) {
    if (principal.departamentoId() != Catalogo.DEPARTAMENTO_ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }
}
