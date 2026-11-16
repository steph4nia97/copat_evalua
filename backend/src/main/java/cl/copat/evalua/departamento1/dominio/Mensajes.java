package cl.copat.evalua.departamento1.dominio;

public final class Mensajes {

  public static final String CREDENCIALES = "Correo o contraseña incorrectos.";
  public static final String SESION = "Debe iniciar sesión.";
  public static final String NOMBRE_REQUERIDO = "Ingrese el nombre de la empresa.";
  public static final String NOMBRE =
      "El nombre de la empresa solo admite letras y números, con un máximo de 100 caracteres.";
  public static final String RUT_REQUERIDO = "Ingrese el RUT.";
  public static final String RUT = "El RUT solo admite números y la letra K, con dígito verificador.";
  public static final String CORREO_REQUERIDO = "Ingrese el correo.";
  public static final String CORREO = "Ingrese un correo electrónico válido.";
  public static final String TELEFONO = "El teléfono debe contener solo dígitos de un número chileno.";
  public static final String FECHA_INICIO = "Ingrese el período de inicio en formato dd/mm/aaaa.";
  public static final String FECHA_FIN = "Ingrese el período de término en formato dd/mm/aaaa.";
  public static final String PERIODO = "La fecha de término no puede ser anterior a la fecha de inicio.";
  public static final String SEDE_REQUERIDA = "Marque la sede que despachó la obra.";
  public static final String SEDE = "Seleccione una sede de despacho.";
  public static final String PLANTILLA_REQUERIDA = "Seleccione una plantilla.";
  public static final String PLANTILLA = "Seleccione Guia evaluacion o Guia evaluacion 2.";
  public static final String DESCRIPCION = "La descripción admite como máximo 300 caracteres.";
  public static final String DUPLICADO = "Esta empresa ya tiene una evaluación para las mismas fechas.";
  public static final String ESTADO = "Estado no válido.";
  public static final String REVISE = "Revise los datos ingresados.";
  public static final String CUERPO = "El cuerpo de la solicitud no es válido.";
  public static final String INESPERADO = "Ocurrió un error inesperado.";
  public static final String NO_ENCONTRADO = "No encontrado.";
  public static final String PROHIBIDO = "No tiene permiso para esta acción.";
  public static final String PDF = "Adjunte la encuesta D-SIG-10-01 completada.";
  public static final String PDF_TIPO = "El archivo debe ser un PDF.";
  public static final String PDF_GUIA =
      "El PDF no corresponde a la encuesta D-SIG-10-01. Descargue la guía base de esta solicitud.";
  public static final String PDF_INCOMPLETO = "El PDF no está completo según la guía.";
  public static final String ESTADO_CREADA = "Solo puede marcar como enviada a cliente una solicitud ingresada.";
  public static final String ESTADO_ENVIADA =
      "Solo puede enviar a revisión una solicitud que ya fue enviada a cliente.";
  public static final String DESCRIPCION_REVISION = "Escriba una nota de seguimiento.";
  public static final String SEGUIMIENTO = "Elija un estado de seguimiento.";
  public static final String DESCRIPCION_REVISION_LARGA = "La descripción admite como máximo 500 caracteres.";
  public static final String EVIDENCIA = "La evidencia debe ser un PDF o una imagen PNG o JPG de hasta 8 MB.";
  public static final String ARCHIVO_GRANDE = "El archivo supera el tamaño permitido de 8 MB.";
  public static final String REVISION_AUSENTE = "Esta solicitud no está en revisión.";
  public static final String RECHAZO = "Ingrese el motivo del rechazo.";
  public static final String RECHAZO_LARGO = "El motivo admite como máximo 500 caracteres.";
  public static final String RECHAZO_ESTADO = "Solo puede rechazar una solicitud que está en revisión.";
  public static final String REENVIO_ESTADO = "Solo puede reenviar una solicitud rechazada.";
  public static final String NOMBRE_USUARIO = "Ingrese el nombre, con un máximo de 80 caracteres.";
  public static final String CLAVE_CORTA = "La contraseña debe tener entre 8 y 72 caracteres.";
  public static final String CLAVE_ACTUAL = "La contraseña actual no coincide.";
  public static final String CLAVE_DISTINTA = "La nueva contraseña debe ser distinta de la actual.";
  public static final String CORREO_REGISTRADO = "Este correo ya está registrado.";
  public static final String DEPARTAMENTO_USUARIO = "Seleccione Departamento 1, 2 o 3.";
  public static final String ENLACE_AUSENTE = "Esta solicitud todavía no tiene enlace. Envíe la encuesta para crearlo.";
  public static final String ENLACE_ESTADO =
      "Solo puede crear el enlace de una solicitud ingresada, enviada a cliente o rechazada.";
  public static final String ENCUESTA_CERRADA = "Esta encuesta ya fue respondida.";
  public static final String ENCUESTA_NOMBRE = "Ingrese su nombre, con un máximo de 120 caracteres.";
  public static final String ENCUESTA_CARGO = "Ingrese su cargo, con un máximo de 120 caracteres.";
  public static final String ENCUESTA_OBRA = "Ingrese el nombre de la obra, con un máximo de 200 caracteres.";
  public static final String ENCUESTA_ESTADO = "Seleccione si la obra está finalizada o en construcción.";
  public static final String ENCUESTA_NOTA = "Seleccione una nota del 1 al 5.";
  public static final String ENCUESTA_COMENTARIO = "El comentario admite como máximo 1000 caracteres.";
  public static final String RECLAMO_NOMBRE = "Ingrese su nombre, con un máximo de 120 caracteres.";
  public static final String RECLAMO_TELEFONO = "Ingrese un teléfono chileno.";
  public static final String RECLAMO_OBRA = "Ingrese el nombre de la obra, con un máximo de 200 caracteres.";
  public static final String RECLAMO_FECHA = "Ingrese la fecha del servicio en formato dd/mm/aaaa.";
  public static final String RECLAMO_EMPRESA = "Ingrese la empresa, con un máximo de 150 caracteres.";
  public static final String RECLAMO_UBICACION = "Ingrese la ubicación, con un máximo de 200 caracteres.";
  public static final String RECLAMO_GUIA = "La guía de despacho admite como máximo 40 caracteres.";
  public static final String RECLAMO_TIPO = "Seleccione el motivo del reclamo.";
  public static final String RECLAMO_DESCRIPCION = "Describa el reclamo, con un máximo de 1000 caracteres.";
  public static final String RECLAMO_PREFERENCIA = "Indique si prefiere teléfono o correo.";
  public static final String RECLAMO_HORARIO = "Seleccione un horario de contacto.";
  public static final String RECLAMO_COMENTARIO = "Los comentarios admiten como máximo 500 caracteres.";
  public static final String RECLAMO_SEGUIMIENTO = "Seleccione el estado del reclamo.";
  public static final String RECLAMO_AREA = "Indique el área responsable, con un máximo de 80 caracteres.";
  public static final String RECLAMO_RESPONSABLE = "Indique la persona responsable, con un máximo de 80 caracteres.";
  public static final String RECLAMO_GESTION = "Ingrese la fecha de gestión en formato dd/mm/aaaa.";
  public static final String RECLAMO_OBS = "Las observaciones admiten como máximo 500 caracteres.";
  public static final String RECLAMO_SOLUCION = "Escriba la solución, con un máximo de 1000 caracteres.";
  public static final String RECLAMO_CIERRE = "Ingrese la fecha de cierre en formato dd/mm/aaaa.";
  public static final String RECLAMO_HORA = "Ingrese la hora de finalización en formato hh:mm.";
  public static final String RECLAMO_SEDE = "Seleccione la sede responsable del reclamo.";
  public static final String EXCEL = "Adjunte un archivo Excel (.xlsx).";
  public static final String EXCEL_CLIENTES = "La primera fila debe incluir RUT y Razón social o Nombre empresa. Las demás columnas, como Guía de despacho, se pueden dejar.";
  public static final String EXCEL_CONTACTOS = "La primera fila debe incluir RUT, Razón social, Nombre contacto y Correo.";

  private Mensajes() {}
}
