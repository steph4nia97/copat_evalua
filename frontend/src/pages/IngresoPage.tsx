import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Fragment, useState } from "react";
import { Navigate } from "react-router-dom";
import { api, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";
import { FormularioSolicitud } from "../components/FormularioSolicitud";
import { esNoAplica, NO_APLICA } from "../domain/reglas";
import { formularioVacio, type SolicitudFormulario } from "../domain/solicitud";
import { CargaDatos } from "./ClientesPage";

type Solicitud = {
  id: number;
  nombreEmpresa: string;
  rut: string;
  correo: string;
  telefono: string | null;
  periodoInicio: string;
  periodoFin: string;
  plantilla: string;
  sedes: string[];
  descripcion: string | null;
  estado: string;
  creadoPor: string;
  motivoRechazo: string | null;
  rechazoPara: string | null;
  desempeno: string | null;
  promedio: string | null;
  seguimiento: string | null;
};

const colores: Record<string, string> = {
  Ingresada: "bg-emerald-100 text-emerald-900",
  "Enviada a cliente": "bg-sky-100 text-sky-900",
  Respondida: "bg-amber-100 text-amber-900",
  Cerrado: "bg-teal-100 text-teal-900",
  Rechazada: "bg-rose-100 text-rose-900",
};

function estadoVisible(solicitud: Solicitud) {
  if (solicitud.seguimiento === "Cerrado") {
    return "Cerrado";
  }
  switch (solicitud.estado) {
    case "solicitud ingresada":
      return "Ingresada";
    case "solicitud enviada a cliente":
      return "Enviada a cliente";
    case "solicitud en revision":
      return "Respondida";
    case "solicitud rechazada":
      return "Rechazada";
    default:
      return solicitud.estado;
  }
}

function valoresSolicitud(solicitud: Solicitud): SolicitudFormulario {
  const telefono = solicitud.telefono ?? "";
  const fijo = /^[2-8]\d{8}$/.test(telefono);
  const celular = /^9\d{8}$/.test(telefono);
  const sinTelefono = esNoAplica(telefono);
  return {
    ...formularioVacio,
    nombreEmpresa: solicitud.nombreEmpresa,
    rut: solicitud.rut,
    correo: esNoAplica(solicitud.correo) ? NO_APLICA : solicitud.correo,
    telefono: sinTelefono ? NO_APLICA : celular ? telefono.slice(1) : fijo ? telefono : "",
    tipoTelefono: fijo ? "fijo" : "celular",
    periodoInicio: solicitud.periodoInicio,
    periodoFin: solicitud.periodoFin,
    plantilla: solicitud.plantilla,
    sedes: solicitud.sedes,
    descripcion: solicitud.descripcion ?? "",
  };
}

const ENLACE_PENDIENTE = "El enlace personal se incluirá al confirmar el envío.";

type DestinatarioEnvio = {
  nombre: string;
  correo: string;
  telefono: string | null;
  mensaje: string;
};

type VistaEnvio = {
  desde: string;
  asunto: string;
  destinatarios: DestinatarioEnvio[];
};

function telefonoWhatsapp(telefono: string | null) {
  if (!telefono) {
    return null;
  }
  const digitos = telefono.replace(/\D/g, "");
  if (digitos.startsWith("56") && digitos.length >= 11) {
    return digitos;
  }
  if (/^[2-9]\d{8}$/.test(digitos)) {
    return `56${digitos}`;
  }
  return null;
}

function detalleError(err: unknown) {
  if (err instanceof ErrorApi) {
    return { mensaje: err.message, campos: Object.values(err.campos) };
  }
  return { mensaje: "No se pudo completar la acción.", campos: [] as string[] };
}

export function IngresoPage() {
  const { usuario, listo } = useSesion();
  const queryClient = useQueryClient();
  const [estado, setEstado] = useState("");
  const [copiado, setCopiado] = useState<number | null>(null);
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [enlaces, setEnlaces] = useState<Record<number, string>>({});
  const [avisos, setAvisos] = useState<Record<number, string>>({});
  const [visible, setVisible] = useState<number | null>(null);
  const [consultados, setConsultados] = useState<Record<number, string | null>>({});
  const [errores, setErrores] = useState<Record<number, { mensaje: string; campos: string[] }>>({});
  const [vista, setVista] = useState<"ingreso" | "carga">("ingreso");
  const [formulario, setFormulario] = useState(false);
  const [edicion, setEdicion] = useState<Solicitud | null>(null);
  const [aviso, setAviso] = useState("");
  const [preparacion, setPreparacion] = useState<number | null>(null);
  const [vistaEnvio, setVistaEnvio] = useState<VistaEnvio | null>(null);
  const opera = usuario?.departamentoId === 1;

  const solicitudes = useQuery({
    queryKey: ["d2-solicitudes", estado],
    queryFn: () =>
      api<Solicitud[]>(`/api/departamento1/solicitudes${estado ? `?estado=${encodeURIComponent(estado)}` : ""}`),
    enabled: opera && vista === "ingreso",
    refetchInterval: vista === "ingreso" ? 15000 : false,
    refetchIntervalInBackground: true,
  });
  const plantillas = useQuery({
    queryKey: ["plantillas"],
    queryFn: () => api<string[]>("/api/departamento1/plantillas"),
    enabled: opera && formulario,
  });
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
    enabled: opera && formulario,
  });

  async function reenviar(id: number) {
    setOcupado(id);
    setErrores((actual) => {
      const siguiente = { ...actual };
      delete siguiente[id];
      return siguiente;
    });
    try {
      await api(`/api/departamento1/solicitudes/${id}/reenviar`, { method: "POST" });
      await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
    } finally {
      setOcupado(null);
    }
  }

  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }

  function limpiarError(id: number) {
    setErrores((actual) => {
      const siguiente = { ...actual };
      delete siguiente[id];
      return siguiente;
    });
  }

  async function verEnlace(id: number) {
    if (visible === id) {
      setVisible(null);
      return;
    }
    setVisible(id);
    if (enlaces[id] || id in consultados) {
      return;
    }
    setOcupado(id);
    limpiarError(id);
    try {
      const respuesta = await api<{ token: string | null; aviso: string }>(`/api/departamento1/solicitudes/${id}/enlace`);
      setConsultados((actual) => ({
        ...actual,
        [id]: respuesta.token ? `${window.location.origin}/encuesta/${respuesta.token}` : null,
      }));
      if (!respuesta.token && respuesta.aviso) {
        setAvisos((actual) => ({ ...actual, [id]: respuesta.aviso }));
      }
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
      setVisible(null);
    } finally {
      setOcupado(null);
    }
  }

  async function prepararEnvio(id: number) {
    setOcupado(id);
    limpiarError(id);
    try {
      const vista = await api<VistaEnvio>(`/api/departamento1/solicitudes/${id}/vista-envio`);
      setPreparacion(id);
      setVistaEnvio(vista);
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
    } finally {
      setOcupado(null);
    }
  }

  async function crearEnlace(id: number) {
    setOcupado(id);
    limpiarError(id);
    try {
      const respuesta = await api<{ token: string; correoEnviado: boolean; destino: string; aviso: string }>(
        `/api/departamento1/solicitudes/${id}/enlace`,
        { method: "POST" },
      );
      const url = `${window.location.origin}/encuesta/${respuesta.token}`;
      setEnlaces((actual) => ({ ...actual, [id]: url }));
      setAvisos((actual) => ({ ...actual, [id]: respuesta.aviso }));
      try {
        await navigator.clipboard.writeText(url);
        setCopiado(id);
      } catch {
        setCopiado(null);
      }
      await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
      setPreparacion(null);
      setVistaEnvio(null);
    } catch (err) {
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
    } finally {
      setOcupado(null);
    }
  }

  async function enviarWhatsapp(id: number, destinatario: DestinatarioEnvio) {
    const fono = telefonoWhatsapp(destinatario.telefono);
    if (!fono) {
      setErrores((actual) => ({
        ...actual,
        [id]: { mensaje: "Este contacto no tiene un teléfono válido para WhatsApp.", campos: [] },
      }));
      return;
    }
    const ventana = window.open("", "_blank");
    setOcupado(id);
    limpiarError(id);
    try {
      const respuesta = await api<{ token: string }>(`/api/departamento1/solicitudes/${id}/enlace?correo=false`, {
        method: "POST",
      });
      const url = `${window.location.origin}/encuesta/${respuesta.token}`;
      const texto = destinatario.mensaje.split(ENLACE_PENDIENTE).join(url);
      const destino = `https://wa.me/${fono}?text=${encodeURIComponent(texto)}`;
      if (ventana) {
        ventana.location.href = destino;
      } else {
        window.location.href = destino;
      }
      setEnlaces((actual) => ({ ...actual, [id]: url }));
      setAvisos((actual) => ({ ...actual, [id]: `Se abrió WhatsApp para ${destinatario.nombre}.` }));
      setPreparacion(null);
      setVistaEnvio(null);
      await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
    } catch (err) {
      ventana?.close();
      setErrores((actual) => ({ ...actual, [id]: detalleError(err) }));
    } finally {
      setOcupado(null);
    }
  }

  async function guardada() {
    const eraEdicion = edicion != null;
    setFormulario(false);
    setEdicion(null);
    setAviso(
      eraEdicion
        ? "La solicitud ingresada quedó actualizada."
        : "La solicitud quedó registrada con estado solicitud ingresada.",
    );
    await queryClient.invalidateQueries({ queryKey: ["d2-solicitudes"] });
  }

  const botonVista = (activa: boolean) =>
    `rounded-md px-4 py-2 font-semibold ${activa ? "bg-pine text-white" : "border border-line bg-card"}`;

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <h1 className="font-serif text-3xl">Solicitudes</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted">
          Desde aquí se ingresa la solicitud, se cargan los clientes y se envía la encuesta.
        </p>
        <div className="mt-5 flex flex-wrap gap-3">
          <button type="button" className={botonVista(vista === "ingreso")} onClick={() => setVista("ingreso")}>
            Ingreso de solicitud
          </button>
          <button type="button" className={botonVista(vista === "carga")} onClick={() => setVista("carga")}>
            Carga de datos
          </button>
        </div>
        {vista === "carga" ? (
          <CargaDatos />
        ) : (
          <>
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <p className="max-w-3xl text-sm text-muted">
            La empresa, el RUT, el correo y el teléfono pueden salir del cruce de los Excel o escribirse a mano. El período, la sede y la plantilla se eligen al ingresar.
          </p>
          <button
            type="button"
            className="rounded-md bg-copper px-4 py-2 font-semibold text-white hover:bg-copper-dark"
            onClick={() => {
              setAviso("");
              setEdicion(null);
              setFormulario(true);
            }}
          >
            Nueva solicitud
          </button>
        </div>
        {aviso && <p className="mt-4 rounded-md bg-pine-soft px-3 py-2 text-sm text-pine">{aviso}</p>}
        {(solicitudes.data ?? []).some((item) => item.estado === "solicitud rechazada" && item.rechazoPara === usuario?.email) && (
          <p className="mt-4 rounded-md bg-rose-100 px-3 py-2 text-sm text-rose-950">
            Departamento 3 rechazó una solicitud que usted envió. Revise el motivo y comparta de nuevo el enlace: el cliente puede corregir la encuesta.
          </p>
        )}

        <div className="mt-5 flex items-center gap-3">
          <label htmlFor="estado" className="text-sm font-semibold">
            Estado
          </label>
          <select
            id="estado"
            value={estado}
            onChange={(evento) => setEstado(evento.target.value)}
            className="rounded-md border border-line bg-card px-3 py-2"
          >
            <option value="">Todas</option>
            <option value="solicitud enviada a cliente">Enviada a cliente</option>
            <option value="solicitud en revision">Respondida</option>
          </select>
        </div>

        <div className="mt-4 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Empresa", "Descripción", "Contacto", "Período", "Sede", "Guía", "Evaluación", "Estado", "Acciones"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">
                    {columna}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(solicitudes.data ?? []).map((solicitud) => {
                const error = errores[solicitud.id];
                const previa = preparacion === solicitud.id ? vistaEnvio : null;
                return (
                  <Fragment key={solicitud.id}>
                  <tr className="border-t border-line align-top">
                    <td className="px-3 py-3">
                      <p className="font-semibold">{solicitud.nombreEmpresa}</p>
                      <p className="mt-1 text-muted">{solicitud.rut}</p>
                      {solicitud.estado === "solicitud rechazada" && solicitud.motivoRechazo && (
                        <p className="mt-2 max-w-xs rounded-md bg-rose-50 px-2 py-1 text-rose-950">
                          Motivo del rechazo: {solicitud.motivoRechazo}
                        </p>
                      )}
                    </td>
                    <td className="max-w-xs px-3 py-3">{solicitud.descripcion?.trim() || "—"}</td>
                    <td className="px-3 py-3">
                      <p>{solicitud.correo}</p>
                      <p className="mt-1">{solicitud.telefono ?? "—"}</p>
                    </td>
                    <td className="whitespace-nowrap px-3 py-3">
                      {solicitud.periodoInicio} – {solicitud.periodoFin}
                    </td>
                    <td className="px-3 py-3">{solicitud.sedes.length ? solicitud.sedes.join(", ") : "Sin sede"}</td>
                    <td className="px-3 py-3">{solicitud.plantilla}</td>
                    <td className="px-3 py-3">
                      {solicitud.desempeno ? `${solicitud.desempeno} · ${solicitud.promedio}` : "—"}
                    </td>
                    <td className="whitespace-nowrap px-3 py-3">
                      <span className={`rounded-full px-2 py-1 text-xs font-semibold ${colores[estadoVisible(solicitud)] ?? ""}`}>
                        {estadoVisible(solicitud)}
                      </span>
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex min-w-56 flex-col items-start gap-2">
                        {solicitud.estado === "solicitud ingresada" && (
                          <button
                            type="button"
                            className="rounded-md border border-line px-3 py-1"
                            onClick={() => {
                              setAviso("");
                              setEdicion(solicitud);
                              setFormulario(true);
                            }}
                          >
                            Editar
                          </button>
                        )}
                        <button
                          type="button"
                          disabled={ocupado === solicitud.id}
                          className="rounded-md border border-line px-3 py-1 disabled:opacity-60"
                          aria-expanded={visible === solicitud.id}
                          onClick={() => void verEnlace(solicitud.id)}
                        >
                          {visible === solicitud.id ? "Ocultar enlace" : "Ver enlace"}
                        </button>
                        {solicitud.estado === "solicitud ingresada" && (
                          <button
                            type="button"
                            disabled={ocupado === solicitud.id}
                            className="rounded-md bg-pine px-3 py-1 font-semibold text-white disabled:opacity-60"
                            onClick={() => void prepararEnvio(solicitud.id)}
                          >
                            Enviar encuesta
                          </button>
                        )}
                        {(enlaces[solicitud.id] || (visible === solicitud.id && consultados[solicitud.id])) && (
                          <input
                            readOnly
                            value={enlaces[solicitud.id] ?? consultados[solicitud.id] ?? ""}
                            aria-label="Enlace de la encuesta"
                            className="w-full rounded-md border border-line bg-white px-2 py-1 text-xs"
                            onFocus={(evento) => evento.currentTarget.select()}
                          />
                        )}
                        {avisos[solicitud.id] && (
                          <p className="text-xs text-pine">{avisos[solicitud.id]}</p>
                        )}
                        {copiado === solicitud.id && enlaces[solicitud.id] && (
                          <p className="text-xs font-semibold text-pine">Enlace copiado</p>
                        )}
                        {solicitud.estado === "solicitud rechazada" && (
                          <button
                            type="button"
                            disabled={ocupado === solicitud.id}
                            className="rounded-md bg-copper px-3 py-1 font-semibold text-white disabled:opacity-60"
                            onClick={() => void reenviar(solicitud.id)}
                          >
                            Volver a enviar a revisión
                          </button>
                        )}
                        {error && (
                          <div role="alert" className="text-xs text-copper-dark">
                            <p>{error.mensaje}</p>
                            {error.campos.length > 0 && (
                              <ul className="mt-1 list-disc pl-4">
                                {error.campos.map((campo) => (
                                  <li key={campo}>{campo}</li>
                                ))}
                              </ul>
                            )}
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                  {previa && (
                    <tr className="border-t border-line">
                      <td colSpan={8} className="bg-paper px-3 py-3">
                        <div className="max-w-2xl rounded-md border border-line bg-card p-3">
                          <p className="text-sm font-semibold">Así se enviará la encuesta</p>
                          <p className="mt-2 text-sm">Desde {previa.desde}</p>
                          {previa.destinatarios.map((destino) => (
                            <p key={destino.correo} className="mt-2 text-sm">
                              Correo {destino.correo}
                              {destino.telefono ? ` · Teléfono ${destino.telefono}` : " · Sin teléfono"}
                            </p>
                          ))}
                          <p className="mt-3 text-sm">Asunto: {previa.asunto}</p>
                          {previa.destinatarios[0] && (
                            <pre className="mt-2 whitespace-pre-wrap rounded-md border border-line bg-paper p-2 font-sans text-sm">
                              {previa.destinatarios[0].mensaje}
                            </pre>
                          )}
                          <p className="mt-2 text-xs text-muted">
                            WhatsApp abre el chat con este texto. Para que la aplicación lo envíe sola hay que conectar WhatsApp Business.
                          </p>
                          <div className="mt-3 flex flex-wrap gap-2">
                            <button
                              type="button"
                              disabled={ocupado === solicitud.id || previa.destinatarios.length === 0}
                              className="rounded-md bg-pine px-3 py-1 text-sm font-semibold text-white disabled:opacity-60"
                              onClick={() => void crearEnlace(solicitud.id)}
                            >
                              Enviar correo
                            </button>
                            {previa.destinatarios.map((destino) => (
                              <button
                                key={`wsp-${destino.correo}`}
                                type="button"
                                disabled={ocupado === solicitud.id || telefonoWhatsapp(destino.telefono) == null}
                                className="rounded-md border border-line px-3 py-1 text-sm font-semibold disabled:opacity-60"
                                onClick={() => void enviarWhatsapp(solicitud.id, destino)}
                              >
                                Enviar por WhatsApp
                              </button>
                            ))}
                            <button
                              type="button"
                              className="rounded-md border border-line px-3 py-1 text-sm"
                              onClick={() => {
                                setPreparacion(null);
                                setVistaEnvio(null);
                              }}
                            >
                              Cerrar
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                  </Fragment>
                );
              })}
              {solicitudes.data?.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-3 py-8 text-center text-muted">
                    No hay solicitudes para este filtro.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
          </>
        )}
      </main>
      {formulario && (
        <FormularioSolicitud
          key={edicion?.id ?? "nueva"}
          plantillas={plantillas.data ?? []}
          sedes={sedes.data ?? []}
          solicitudId={edicion?.id}
          valores={edicion ? valoresSolicitud(edicion) : undefined}
          onCerrar={() => {
            setFormulario(false);
            setEdicion(null);
          }}
          onGuardada={() => void guardada()}
        />
      )}
    </div>
  );
}
