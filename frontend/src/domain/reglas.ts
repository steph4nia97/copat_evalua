export const mensajes = {
  nombreRequerido: "Ingrese el nombre de la empresa.",
  nombre: "El nombre de la empresa solo admite letras y números, con un máximo de 100 caracteres.",
  rutRequerido: "Ingrese el RUT.",
  rut: "El RUT no supera la validación módulo 11.",
  correoRequerido: "Ingrese el correo.",
  correo: "Ingrese un correo electrónico válido.",
  telefono: "El teléfono debe contener solo dígitos de un número chileno.",
  fechaInicio: "Ingrese el período de inicio en formato dd/mm/aaaa.",
  fechaFin: "Ingrese el período de término en formato dd/mm/aaaa.",
  periodo: "La fecha de término no puede ser anterior a la fecha de inicio.",
  plantillaRequerida: "Seleccione una plantilla.",
  plantilla: "Seleccione Guia evaluacion o Guia evaluacion 2.",
  descripcion: "La descripción admite como máximo 300 caracteres.",
  duplicado: "Esta empresa ya tiene una evaluación para las mismas fechas.",
  credenciales: "Correo o contraseña incorrectos.",
  revise: "Revise los datos ingresados.",
};

const NOMBRE = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]+)*$/;
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TELEFONO = /^(?:56)?[2-9]\d{8}$/;

export const PLANTILLAS = ["Guia evaluacion", "Guia evaluacion 2"] as const;

export function normalizarNombre(valor: string): string {
  return valor.trim().replace(/\s+/g, " ");
}

export function nombreValido(valor: string): boolean {
  const normalizado = normalizarNombre(valor);
  return normalizado.length > 0 && normalizado.length <= 100 && NOMBRE.test(normalizado);
}

export function rutValido(valor: string): boolean {
  const limpio = valor.replace(/[.\-\s]/g, "").toUpperCase();
  if (!/^\d{7,8}[0-9K]$/.test(limpio)) {
    return false;
  }
  return limpio.slice(-1) === calcularDv(limpio.slice(0, -1));
}

export function telefonoValido(valor: string): boolean {
  const texto = valor.trim();
  return texto.length === 0 || TELEFONO.test(texto);
}

export function correoValido(valor: string): boolean {
  const texto = valor.trim();
  return texto.length > 0 && texto.length <= 254 && CORREO.test(texto);
}

export function fechaValida(valor: string): boolean {
  const coincidencia = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(valor.trim());
  if (!coincidencia) {
    return false;
  }
  const dia = Number(coincidencia[1]);
  const mes = Number(coincidencia[2]);
  const anio = Number(coincidencia[3]);
  const fecha = new Date(anio, mes - 1, dia);
  return fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia;
}

export function compararFechas(inicio: string, fin: string): number {
  const [diaInicio, mesInicio, anioInicio] = inicio.split("/").map(Number);
  const [diaFin, mesFin, anioFin] = fin.split("/").map(Number);
  return anioInicio - anioFin || mesInicio - mesFin || diaInicio - diaFin;
}

export function enmascararFecha(valor: string): string {
  const digitos = valor.replace(/\D/g, "").slice(0, 8);
  const partes = [digitos.slice(0, 2), digitos.slice(2, 4), digitos.slice(4, 8)].filter(Boolean);
  return partes.join("/");
}

function calcularDv(cuerpo: string): string {
  let suma = 0;
  let factor = 2;
  for (let i = cuerpo.length - 1; i >= 0; i -= 1) {
    suma += Number(cuerpo[i]) * factor;
    factor = factor === 7 ? 2 : factor + 1;
  }
  const resto = 11 - (suma % 11);
  if (resto === 11) {
    return "0";
  }
  if (resto === 10) {
    return "K";
  }
  return String(resto);
}
