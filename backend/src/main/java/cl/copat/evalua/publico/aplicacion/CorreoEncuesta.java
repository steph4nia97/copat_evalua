package cl.copat.evalua.publico.aplicacion;

import cl.copat.evalua.departamento1.dominio.Rut;
import cl.copat.evalua.departamento1.persistencia.Contacto;
import cl.copat.evalua.departamento1.persistencia.ContactoRepository;
import cl.copat.evalua.departamento1.persistencia.Solicitud;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoEncuesta {

  private final ContactoRepository contactos;
  private final ObjectProvider<JavaMailSender> correos;
  private final String publica;
  private final String desde;

  public CorreoEncuesta(
      ContactoRepository contactos,
      ObjectProvider<JavaMailSender> correos,
      @Value("${copat.publica}") String publica,
      @Value("${copat.correo.desde}") String desde) {
    this.contactos = contactos;
    this.correos = correos;
    this.publica = publica.endsWith("/") ? publica.substring(0, publica.length() - 1) : publica;
    this.desde = desde;
  }

  public Resultado enviar(Solicitud solicitud, String token) {
    List<Contacto> destinos = contactos.findByRutOrderByIdAsc(solicitud.getRut());
    if (destinos.isEmpty()) {
      return new Resultado(
          false,
          "",
          "No hay contacto para el RUT " + Rut.formatear(solicitud.getRut()) + ". El enlace quedó creado.");
    }
    String destino = String.join(", ", destinos.stream().map(Contacto::getCorreo).toList());
    JavaMailSender sender = correos.getIfAvailable();
    if (sender == null) {
      return new Resultado(false, destino, "Falta configurar el servidor de correo. El enlace quedó creado.");
    }
    String url = publica + "/encuesta/" + token;
    try {
      for (Contacto contacto : destinos) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(desde);
        mensaje.setTo(contacto.getCorreo());
        mensaje.setSubject("Encuesta de satisfacción Copat — " + solicitud.getNombreEmpresa());
        mensaje.setText("""
            Hola %s,

            Copat le invita a responder la encuesta de satisfacción del servicio de %s.

            %s

            El enlace es personal y no requiere una cuenta.
            """.formatted(contacto.getNombre(), solicitud.getNombreEmpresa(), url));
        sender.send(mensaje);
      }
    } catch (MailException ex) {
      return new Resultado(false, destino, "No se pudo enviar el correo a " + destino + ". El enlace quedó creado.");
    }
    return new Resultado(true, destino, "Se envió la encuesta a " + destino + ".");
  }

  public record Resultado(boolean enviado, String destino, String aviso) {}
}
