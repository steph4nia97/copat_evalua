export const mensajes = {
  nombreRequerido: "Ingrese el nombre de la empresa.",
  nombre: "El nombre de la empresa admite letras, números y signos de razón social, con un máximo de 100 caracteres.",
  rutRequerido: "Ingrese el RUT.",
  rut: "El RUT solo admite números y la letra K, con dígito verificador.",
  correoRequerido: "Ingrese el correo.",
  correo: "Ingrese un correo electrónico válido.",
  telefono: "Ingrese solo números: 8 dígitos para celular o 9 para un fijo chileno.",
  fechaInicio: "Ingrese el período de inicio en formato dd/mm/aaaa.",
  fechaFin: "Ingrese el período de término en formato dd/mm/aaaa.",
  periodo: "La fecha de término no puede ser anterior a la fecha de inicio.",
  sedeRequerida: "Marque la sede que despachó la obra.",
  sede: "Seleccione una sede de despacho.",
  plantillaRequerida: "Seleccione una plantilla.",
  plantilla: "Seleccione Guia evaluacion o Guia evaluacion 2.",
  descripcion: "La descripción admite como máximo 300 caracteres.",
  duplicado: "Esta empresa ya tiene una evaluación para las mismas fechas.",
  credenciales: "Correo o contraseña incorrectos.",
  revise: "Revise los datos ingresados.",
};

const NOMBRE = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9.,&'()/-]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9.,&'()/-]+)*$/;
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const NO_APLICA = "No aplica";

export function esNoAplica(valor: string | null | undefined): boolean {
  return (valor ?? "").trim().toLowerCase() === "no aplica";
}

export const PLANTILLAS = ["Guia evaluacion", "Guia evaluacion 2"] as const;

export const SEDES = ["Antofagasta", "Mejillones", "Calama", "María Elena", "Tocopilla", "Taltal"] as const;

export function normalizarNombre(valor: string): string {
  return valor.trim().replace(/\s+/g, " ");
}

export function nombreValido(valor: string): boolean {
  const normalizado = normalizarNombre(valor);
  return normalizado.length > 0 && normalizado.length <= 100 && NOMBRE.test(normalizado);
}

export function enmascararRut(valor: string): string {
  const crudo = valor.toUpperCase().replace(/[^0-9K]/g, "");
  const sinK = crudo.replace(/K/g, "");
  const limpio = (crudo.includes("K") ? `${sinK.slice(0, 8)}K` : sinK).slice(0, 9);
  if (limpio.length <= 1) {
    return limpio;
  }
  const cuerpo = limpio.slice(0, -1);
  const conPuntos = cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${conPuntos}-${limpio.slice(-1)}`;
}

export function rutValido(valor: string): boolean {
  const limpio = valor.replace(/[.\-\s]/g, "").toUpperCase();
  if (!/^\d{7,8}[0-9K]$/.test(limpio)) {
    return false;
  }
  return limpio.slice(-1) === calcularDv(limpio.slice(0, -1));
}

export type TipoTelefono = "celular" | "fijo";

export function enmascararTelefono(valor: string, tipo: TipoTelefono): string {
  let digitos = valor.replace(/\D/g, "");
  if (tipo === "celular") {
    if (digitos.length > 8 && digitos.startsWith("569")) {
      digitos = digitos.slice(3);
    } else if (digitos.length > 8 && digitos.startsWith("9")) {
      digitos = digitos.slice(1);
    }
    return digitos.slice(0, 8);
  }
  if (digitos.length > 9 && digitos.startsWith("56")) {
    digitos = digitos.slice(2);
  }
  return digitos.slice(0, 9);
}

export function telefonoValido(valor: string, tipo: TipoTelefono): boolean {
  const texto = valor.trim();
  if (texto.length === 0 || esNoAplica(texto)) {
    return true;
  }
  return tipo === "celular" ? /^\d{8}$/.test(texto) : /^[2-8]\d{8}$/.test(texto);
}

export function componerTelefono(valor: string, tipo: TipoTelefono): string {
  if (esNoAplica(valor)) {
    return NO_APLICA;
  }
  const texto = enmascararTelefono(valor, tipo);
  if (!telefonoValido(texto, tipo) || texto.length === 0) {
    return "";
  }
  return tipo === "celular" ? `9${texto}` : texto;
}

export function correoValido(valor: string): boolean {
  const texto = valor.trim();
  if (esNoAplica(texto)) {
    return true;
  }
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
