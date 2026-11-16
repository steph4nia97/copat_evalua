import { z } from "zod";
import {
  compararFechas,
  correoValido,
  fechaValida,
  mensajes,
  nombreValido,
  PLANTILLAS,
  rutValido,
  SEDES,
  telefonoValido,
  type TipoTelefono,
} from "./reglas";

export const solicitudSchema = z
  .object({
    nombreEmpresa: z.string(),
    rut: z.string(),
    correo: z.string(),
    telefono: z.string(),
    tipoTelefono: z.enum(["celular", "fijo"]),
    periodoInicio: z.string(),
    periodoFin: z.string(),
    plantilla: z.string(),
    sedes: z.array(z.string()),
    descripcion: z.string(),
  })
  .superRefine((valores, ctx) => {
    if (!valores.nombreEmpresa.trim()) {
      ctx.addIssue({ code: "custom", path: ["nombreEmpresa"], message: mensajes.nombreRequerido });
    } else if (!nombreValido(valores.nombreEmpresa)) {
      ctx.addIssue({ code: "custom", path: ["nombreEmpresa"], message: mensajes.nombre });
    }
    if (!valores.rut.trim()) {
      ctx.addIssue({ code: "custom", path: ["rut"], message: mensajes.rutRequerido });
    } else if (!rutValido(valores.rut)) {
      ctx.addIssue({ code: "custom", path: ["rut"], message: mensajes.rut });
    }
    if (!valores.correo.trim()) {
      ctx.addIssue({ code: "custom", path: ["correo"], message: mensajes.correoRequerido });
    } else if (!correoValido(valores.correo)) {
      ctx.addIssue({ code: "custom", path: ["correo"], message: mensajes.correo });
    }
    if (!telefonoValido(valores.telefono, valores.tipoTelefono)) {
      ctx.addIssue({ code: "custom", path: ["telefono"], message: mensajes.telefono });
    }
    const inicioValido = fechaValida(valores.periodoInicio);
    const finValido = fechaValida(valores.periodoFin);
    if (!inicioValido) {
      ctx.addIssue({ code: "custom", path: ["periodoInicio"], message: mensajes.fechaInicio });
    }
    if (!finValido) {
      ctx.addIssue({ code: "custom", path: ["periodoFin"], message: mensajes.fechaFin });
    }
    if (inicioValido && finValido && compararFechas(valores.periodoInicio.trim(), valores.periodoFin.trim()) > 0) {
      ctx.addIssue({ code: "custom", path: ["periodoFin"], message: mensajes.periodo });
    }
    if (valores.sedes.length === 0) {
      ctx.addIssue({ code: "custom", path: ["sedes"], message: mensajes.sedeRequerida });
    } else if (valores.sedes.some((sede) => !SEDES.includes(sede as (typeof SEDES)[number]))) {
      ctx.addIssue({ code: "custom", path: ["sedes"], message: mensajes.sede });
    }
    if (!valores.plantilla) {
      ctx.addIssue({ code: "custom", path: ["plantilla"], message: mensajes.plantillaRequerida });
    } else if (!PLANTILLAS.includes(valores.plantilla as (typeof PLANTILLAS)[number])) {
      ctx.addIssue({ code: "custom", path: ["plantilla"], message: mensajes.plantilla });
    }
    if (valores.descripcion.trim().length > 300) {
      ctx.addIssue({ code: "custom", path: ["descripcion"], message: mensajes.descripcion });
    }
  });

export type SolicitudFormulario = z.infer<typeof solicitudSchema>;

export const formularioVacio: SolicitudFormulario = {
  nombreEmpresa: "",
  rut: "",
  correo: "",
  telefono: "",
  tipoTelefono: "celular" satisfies TipoTelefono,
  periodoInicio: "",
  periodoFin: "",
  plantilla: "",
  sedes: [],
  descripcion: "",
};
