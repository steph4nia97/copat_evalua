package cl.copat.evalua.departamento1.aplicacion;

import cl.copat.evalua.departamento1.api.CargaResponse;
import cl.copat.evalua.departamento1.api.CruceResponse;
import cl.copat.evalua.departamento1.dominio.Catalogo;
import cl.copat.evalua.departamento1.dominio.Correo;
import cl.copat.evalua.departamento1.dominio.Mensajes;
import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.persistencia.Cliente;
import cl.copat.evalua.departamento1.persistencia.ClienteRepository;
import cl.copat.evalua.departamento1.persistencia.Contacto;
import cl.copat.evalua.departamento1.persistencia.ContactoRepository;
import cl.copat.evalua.departamento1.persistencia.Usuario;
import cl.copat.evalua.departamento1.persistencia.UsuarioRepository;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import cl.copat.evalua.shared.aplicacion.ActividadService;
import cl.copat.evalua.shared.web.ReglaNegocioException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogoClienteService {

  private static final int AVISOS = 30;

  private final ClienteRepository clientes;
  private final ContactoRepository contactos;
  private final UsuarioRepository usuarios;
  private final ActividadService actividad;

  public CatalogoClienteService(
      ClienteRepository clientes,
      ContactoRepository contactos,
      UsuarioRepository usuarios,
      ActividadService actividad) {
    this.clientes = clientes;
    this.contactos = contactos;
    this.usuarios = usuarios;
    this.actividad = actividad;
  }

  @Transactional
  public CargaResponse cargarClientes(MultipartFile archivo, UsuarioPrincipal principal) {
    exigir(principal);
    HojaExcel hoja = abrir(archivo);
    String colRut = columna(hoja, "rut");
    String colRazon = columna(hoja, "razon social");
    String colNombre = columna(hoja, "nombre empresa", "nombre de la empresa", "empresa");
    if (colRut == null || (colRazon == null && colNombre == null)) {
      throw new ReglaNegocioException(Mensajes.EXCEL_CLIENTES);
    }
    int guardados = 0;
    int omitidos = 0;
    List<String> avisos = new ArrayList<>();
    Set<String> vistos = new LinkedHashSet<>();
    for (Map<String, String> fila : hoja.filas()) {
      String rut = Rut.normalizar(fila.get(colRut));
      String empresa = empresaCliente(fila, colRazon, colNombre);
      if (!Rut.esValido(fila.get(colRut)) || empresa.isEmpty() || empresa.length() > 150) {
        omitidos++;
        avisar(avisos, "Fila " + fila.get("__fila") + ": RUT, razón social o nombre empresa no válidos.");
        continue;
      }
      Cliente actual = clientes.findById(rut).orElse(null);
      if (actual == null) {
        clientes.save(new Cliente(rut, empresa));
      } else {
        actual.renombrar(empresa);
      }
      if (vistos.add(rut)) {
        guardados++;
      }
    }
    clientes.flush();
    anotarCarga(principal, Catalogo.ACCION_CARGA_CLIENTES, guardados, "clientes");
    return new CargaResponse(guardados, omitidos, avisos);
  }

  @Transactional
  public CargaResponse cargarContactos(MultipartFile archivo, UsuarioPrincipal principal) {
    exigir(principal);
    HojaExcel hoja = abrir(archivo);
    String colRut = columna(hoja, "rut");
    String colNombre = columna(hoja, "nombre", "nombre contacto");
    String colEmpresa = columna(hoja, "empresa", "razon social");
    String colCorreo = columna(hoja, "contacto", "correo", "email", "e mail", "mail");
    String colTelefono = columna(hoja, "telefono", "fono");
    if (colRut == null || colNombre == null || colEmpresa == null || colCorreo == null) {
      throw new ReglaNegocioException(Mensajes.EXCEL_CONTACTOS);
    }
    Map<String, Contacto> porClave = new LinkedHashMap<>();
    for (Contacto contacto : contactos.findAll()) {
      porClave.put(contacto.getRut() + "|" + contacto.getCorreo(), contacto);
    }
    int guardados = 0;
    int omitidos = 0;
    List<String> avisos = new ArrayList<>();
    for (Map<String, String> fila : hoja.filas()) {
      String rut = Rut.normalizar(fila.get(colRut));
      String nombre = texto(fila.get(colNombre));
      String empresa = texto(fila.get(colEmpresa));
      String correo = Correo.normalizar(fila.get(colCorreo));
      String telefono = colTelefono == null ? "" : texto(fila.get(colTelefono));
      if (telefono.length() > 20) {
        telefono = telefono.substring(0, 20);
      }
      if (!Rut.esValido(fila.get(colRut)) || nombre.isEmpty() || nombre.length() > 120
          || empresa.isEmpty() || empresa.length() > 150 || !Correo.esValido(correo)) {
        omitidos++;
        avisar(avisos, "Fila " + fila.get("__fila") + ": revise RUT, razón social, nombre contacto y correo.");
        continue;
      }
      if (!clientes.existsById(rut)) {
        omitidos++;
        avisar(avisos, "Fila " + fila.get("__fila") + ": el RUT " + Rut.formatear(rut) + " no está entre los clientes seleccionados.");
        continue;
      }
      Contacto actual = porClave.get(rut + "|" + correo);
      if (actual == null) {
        Contacto nuevo = contactos.save(new Contacto(rut, nombre, empresa, correo, telefono.isEmpty() ? null : telefono));
        porClave.put(rut + "|" + correo, nuevo);
      } else {
        actual.actualizar(nombre, empresa, telefono.isEmpty() ? null : telefono);
      }
      guardados++;
    }
    contactos.flush();
    anotarCarga(principal, Catalogo.ACCION_CARGA_CONTACTOS, guardados, "contactos");
    return new CargaResponse(guardados, omitidos, avisos);
  }

  public List<CruceResponse> cruce(UsuarioPrincipal principal) {
    exigir(principal);
    Map<String, List<Contacto>> porRut = new LinkedHashMap<>();
    for (Contacto contacto : contactos.findAllByOrderByRutAscIdAsc()) {
      porRut.computeIfAbsent(contacto.getRut(), clave -> new ArrayList<>()).add(contacto);
    }
    List<CruceResponse> cruce = new ArrayList<>();
    for (Cliente cliente : clientes.findAll()) {
      List<Contacto> delCliente = porRut.getOrDefault(cliente.getRut(), List.of());
      if (delCliente.isEmpty()) {
        cruce.add(new CruceResponse(Rut.formatear(cliente.getRut()), cliente.getNombreEmpresa(), null, null, null, null));
        continue;
      }
      for (Contacto contacto : delCliente) {
        cruce.add(new CruceResponse(
            Rut.formatear(cliente.getRut()),
            cliente.getNombreEmpresa(),
            contacto.getNombre(),
            contacto.getEmpresa(),
            contacto.getCorreo(),
            contacto.getTelefono()));
      }
    }
    cruce.sort((a, b) -> a.empresa().compareToIgnoreCase(b.empresa()));
    return cruce;
  }

  private static HojaExcel abrir(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ReglaNegocioException(Mensajes.EXCEL);
    }
    String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
    if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
      throw new ReglaNegocioException(Mensajes.EXCEL);
    }
    try {
      return HojaExcel.leer(archivo.getInputStream());
    } catch (IOException ex) {
      throw new ReglaNegocioException(Mensajes.EXCEL);
    }
  }

  private static String columna(HojaExcel hoja, String... opciones) {
    for (String opcion : opciones) {
      if (hoja.tiene(HojaExcel.clave(opcion))) {
        return HojaExcel.clave(opcion);
      }
    }
    return null;
  }

  private static void avisar(List<String> avisos, String texto) {
    if (avisos.size() < AVISOS) {
      avisos.add(texto);
    }
  }

  private static String empresaCliente(Map<String, String> fila, String colRazon, String colNombre) {
    String razon = colRazon == null ? "" : texto(fila.get(colRazon));
    if (!razon.isEmpty()) {
      return razon;
    }
    return colNombre == null ? "" : texto(fila.get(colNombre));
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor.trim();
  }

  private void anotarCarga(UsuarioPrincipal principal, String accion, int guardados, String unidad) {
    if (guardados <= 0) {
      return;
    }
    Usuario usuario = usuarios.findById(principal.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Mensajes.SESION));
    actividad.anotar(null, usuario, accion, guardados + " " + unidad);
  }

  private static void exigir(UsuarioPrincipal principal) {
    if (!Catalogo.operaSolicitudes(principal.departamentoId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, Mensajes.PROHIBIDO);
    }
  }
}
