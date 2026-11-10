import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../api/client";
import { useSesion } from "../auth/sesion";
import { FormularioSolicitud } from "../components/FormularioSolicitud";

type Solicitud = {
  id: number;
  nombreEmpresa: string;
  rut: string;
  correo: string;
  telefono: string | null;
  periodoInicio: string;
  periodoFin: string;
  plantilla: string;
  descripcion: string | null;
  estado: string;
  creadoPor: string;
};

type Movimiento = {
  id: number;
  nombreEmpresa: string;
  periodoInicio: string;
  periodoFin: string;
  fecha: string;
  hora: string;
  usuario: string;
  accion: string;
};

const colores: Record<string, string> = {
  "solicitud creada": "bg-emerald-100 text-emerald-900",
  "solicitud enviada a cliente": "bg-sky-100 text-sky-900",
  "solicitud en revision": "bg-amber-100 text-amber-900",
  "solicitud aprobada": "bg-teal-100 text-teal-900",
  rechazada: "bg-rose-100 text-rose-900",
};

export function SolicitudesPage() {
  const { usuario, listo, salir } = useSesion();
  const queryClient = useQueryClient();
  const [estado, setEstado] = useState("");
  const [formulario, setFormulario] = useState(false);
  const [aviso, setAviso] = useState("");

  const estados = useQuery({
    queryKey: ["estados"],
    queryFn: () => api<string[]>("/api/departamento1/estados"),
    enabled: Boolean(usuario),
  });
  const plantillas = useQuery({
    queryKey: ["plantillas"],
    queryFn: () => api<string[]>("/api/departamento1/plantillas"),
    enabled: Boolean(usuario),
  });
  const solicitudes = useQuery({
    queryKey: ["solicitudes", estado],
    queryFn: () => api<Solicitud[]>(`/api/departamento1/solicitudes${estado ? `?estado=${encodeURIComponent(estado)}` : ""}`),
    enabled: Boolean(usuario),
  });
  const bitacora = useQuery({
    queryKey: ["bitacora"],
    queryFn: () => api<Movimiento[]>("/api/departamento1/bitacora"),
    enabled: Boolean(usuario),
  });

  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }

  async function creada() {
    setFormulario(false);
    setAviso("La solicitud quedó registrada con estado solicitud creada.");
    await queryClient.invalidateQueries({ queryKey: ["solicitudes"] });
    await queryClient.invalidateQueries({ queryKey: ["bitacora"] });
  }

  return (
    <div className="min-h-screen">
      <header className="bg-pine text-paper">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-4">
          <div>
            <p className="font-serif text-2xl">Copat evalua</p>
            <p className="text-sm text-pine-soft">Departamento 1</p>
          </div>
          <div className="flex items-center gap-4 text-sm">
            <span>
              {usuario.nombre} ({usuario.email})
            </span>
            <button type="button" className="rounded-md border border-white/30 px-3 py-1" onClick={() => void salir()}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="font-serif text-3xl">Solicitudes</h1>
            <p className="text-sm text-muted">Evaluaciones del departamento.</p>
          </div>
          <button
            type="button"
            className="rounded-md bg-copper px-4 py-2 font-semibold text-white hover:bg-copper-dark"
            onClick={() => {
              setAviso("");
              setFormulario(true);
            }}
          >
            Ingresar solicitud
          </button>
        </div>

        {aviso && <p className="mt-4 rounded-md bg-pine-soft px-3 py-2 text-sm text-pine">{aviso}</p>}

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
            {(estados.data ?? []).map((opcion) => (
              <option key={opcion} value={opcion}>
                {opcion}
              </option>
            ))}
          </select>
        </div>

        <div className="mt-4 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Empresa", "RUT", "Correo", "Teléfono", "Período", "Plantilla", "Estado"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">
                    {columna}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(solicitudes.data ?? []).map((solicitud) => (
                <tr key={solicitud.id} className="border-t border-line align-top">
                  <td className="px-3 py-3">
                    <p className="font-semibold">{solicitud.nombreEmpresa}</p>
                    {solicitud.descripcion && <p className="mt-1 max-w-xs text-muted">{solicitud.descripcion}</p>}
                  </td>
                  <td className="whitespace-nowrap px-3 py-3">{solicitud.rut}</td>
                  <td className="px-3 py-3">{solicitud.correo}</td>
                  <td className="whitespace-nowrap px-3 py-3">{solicitud.telefono ?? "—"}</td>
                  <td className="px-3 py-3 whitespace-nowrap">
                    {solicitud.periodoInicio} – {solicitud.periodoFin}
                  </td>
                  <td className="px-3 py-3">{solicitud.plantilla}</td>
                  <td className="whitespace-nowrap px-3 py-3">
                    <span className={`rounded-full px-2 py-1 text-xs font-semibold ${colores[solicitud.estado] ?? ""}`}>
                      {solicitud.estado}
                    </span>
                  </td>
                </tr>
              ))}
              {solicitudes.data?.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-3 py-8 text-center text-muted">
                    No hay solicitudes para este filtro.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <h2 className="mt-8 font-serif text-2xl">Bitácora</h2>
        <p className="text-sm text-muted">Registro automático con fecha, hora y usuario que ingresó la solicitud.</p>
        <div className="mt-3 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Fecha", "Hora", "Usuario", "Acción", "Empresa", "Período"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">
                    {columna}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(bitacora.data ?? []).map((movimiento) => (
                <tr key={movimiento.id} className="border-t border-line">
                  <td className="whitespace-nowrap px-3 py-3">{movimiento.fecha}</td>
                  <td className="whitespace-nowrap px-3 py-3">{movimiento.hora}</td>
                  <td className="px-3 py-3">{movimiento.usuario}</td>
                  <td className="whitespace-nowrap px-3 py-3">{movimiento.accion}</td>
                  <td className="px-3 py-3">{movimiento.nombreEmpresa}</td>
                  <td className="px-3 py-3 whitespace-nowrap">
                    {movimiento.periodoInicio} – {movimiento.periodoFin}
                  </td>
                </tr>
              ))}
              {bitacora.data?.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-3 py-8 text-center text-muted">
                    Aún no hay movimientos en la bitácora.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </main>

      {formulario && (
        <FormularioSolicitud
          plantillas={plantillas.data ?? []}
          onCerrar={() => setFormulario(false)}
          onCreada={() => void creada()}
        />
      )}
    </div>
  );
}
