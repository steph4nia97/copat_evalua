package cl.copat.evalua.publico.api;

import cl.copat.evalua.publico.aplicacion.EncuestaPublicaService;
import cl.copat.evalua.publico.aplicacion.ReclamoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico")
public class PublicoController {

  private final EncuestaPublicaService encuestas;
  private final ReclamoService reclamos;

  public PublicoController(EncuestaPublicaService encuestas, ReclamoService reclamos) {
    this.encuestas = encuestas;
    this.reclamos = reclamos;
  }

  @GetMapping("/encuestas/{token}")
  public EncuestaPublicaResponse ver(@PathVariable String token) {
    return encuestas.ver(token);
  }

  @PostMapping("/encuestas/{token}")
  public ResponseEntity<Void> responder(@PathVariable String token, @RequestBody RespuestaEncuestaRequest body) {
    encuestas.responder(token, body);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reclamos")
  public RecibidoResponse reclamo(@RequestBody ReclamoRequest body) {
    return new RecibidoResponse(reclamos.recibir(body));
  }
}
