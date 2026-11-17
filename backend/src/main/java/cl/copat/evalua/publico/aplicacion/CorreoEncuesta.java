package cl.copat.evalua.publico.aplicacion;

import cl.copat.evalua.departamento1.dominio.NoAplica;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import cl.copat.evalua.publico.api.VistaEnvioResponse;
import cl.copat.evalua.publico.api.VistaEnvioResponse.DestinatarioEnvio;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoEncuesta {

  private final ObjectProvider<JavaMailSender> correos;
  private final String publica;
  private final String desde;

  public CorreoEncuesta(
      ObjectProvider<JavaMailSender> correos,
      @Value("${copat.publica}") String publica,
      @Value("${copat.correo.desde}") String desde) {
    this.correos = correos;
    this.publica = publica.endsWith("/") ? publica.substring(0, publica.length() - 1) : publica;
    this.desde = desde;
  }

  public static final String ENLACE_PENDIENTE = "El enlace personal se incluirá al confirmar el envío.";

  public VistaEnvioResponse vista(Solicitud solicitud, String token) {
    String linea = token == null ? ENLACE_PENDIENTE : publica + "/encuesta/" + token;
    DestinatarioEnvio destinatario = new DestinatarioEnvio(
        solicitud.getNombreEmpresa(),
        solicitud.getCorreo(),
        solicitud.getTelefono(),
        mensaje(solicitud.getNombreEmpresa(), linea));
    return new VistaEnvioResponse(desde, asunto(solicitud.getNombreEmpresa()), List.of(destinatario));
  }

  public Resultado enviar(Solicitud solicitud, String token) {
    String destino = solicitud.getCorreo() == null ? "" : solicitud.getCorreo();
    if (destino.isBlank() || NoAplica.es(destino)) {
      return new Resultado(false, "", "La solicitud no tiene correo. El enlace quedó creado.");
    }
    JavaMailSender sender = correos.getIfAvailable();
    if (sender == null) {
      return new Resultado(false, destino, "Falta configurar el servidor de correo. El enlace quedó creado.");
    }
    String url = publica + "/encuesta/" + token;
    try {
      SimpleMailMessage correo = new SimpleMailMessage();
      correo.setFrom(desde);
      correo.setTo(destino);
      correo.setSubject(asunto(solicitud.getNombreEmpresa()));
      correo.setText(mensaje(solicitud.getNombreEmpresa(), url));
      sender.send(correo);
    } catch (MailException ex) {
      return new Resultado(false, destino, "No se pudo enviar el correo a " + destino + ". El enlace quedó creado.");
    }
    return new Resultado(true, destino, "Se envió la encuesta a " + destino + ".");
  }

  private static String asunto(String empresa) {
    return "Encuesta de satisfacción Copat — " + empresa;
  }

  private static String mensaje(String empresa, String url) {
    return """
        Hola,

        Copat le invita a responder la encuesta de satisfacción del servicio de %s.

        %s

        El enlace es personal y no requiere una cuenta.
        """.formatted(empresa, url);
  }

  public record Resultado(boolean enviado, String destino, String aviso) {}
}
