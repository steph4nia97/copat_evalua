package cl.copat.evalua.departamento1.api;

import cl.copat.evalua.departamento1.aplicacion.CatalogoClienteService;
import cl.copat.evalua.departamento1.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/departamento1")
public class CatalogoClienteController {

  private final CatalogoClienteService catalogo;

  public CatalogoClienteController(CatalogoClienteService catalogo) {
    this.catalogo = catalogo;
  }

  @GetMapping("/cruce")
  public List<CruceResponse> cruce(Authentication authentication) {
    return catalogo.cruce(principal(authentication));
  }

  @PostMapping(value = "/clientes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CargaResponse clientes(@RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
    return catalogo.cargarClientes(archivo, principal(authentication));
  }

  @PostMapping(value = "/contactos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CargaResponse contactos(@RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
    return catalogo.cargarContactos(archivo, principal(authentication));
  }

  private static UsuarioPrincipal principal(Authentication authentication) {
    return (UsuarioPrincipal) authentication.getPrincipal();
  }
}
