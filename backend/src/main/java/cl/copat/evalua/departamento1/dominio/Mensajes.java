package cl.copat.evalua.departamento1.dominio;

public final class Mensajes {

  public static final String CREDENCIALES = "Correo o contraseña incorrectos.";
  public static final String SESION = "Debe iniciar sesión.";
  public static final String NOMBRE_REQUERIDO = "Ingrese el nombre de la empresa.";
  public static final String NOMBRE =
      "El nombre de la empresa solo admite letras y números, con un máximo de 100 caracteres.";
  public static final String RUT_REQUERIDO = "Ingrese el RUT.";
  public static final String RUT = "El RUT no supera la validación módulo 11.";
  public static final String CORREO_REQUERIDO = "Ingrese el correo.";
  public static final String CORREO = "Ingrese un correo electrónico válido.";
  public static final String TELEFONO = "El teléfono debe contener solo dígitos de un número chileno.";
  public static final String FECHA_INICIO = "Ingrese el período de inicio en formato dd/mm/aaaa.";
  public static final String FECHA_FIN = "Ingrese el período de término en formato dd/mm/aaaa.";
  public static final String PERIODO = "La fecha de término no puede ser anterior a la fecha de inicio.";
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

  private Mensajes() {}
}
