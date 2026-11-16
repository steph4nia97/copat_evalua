import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../api/client";
import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";
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
  sedes: string[];
  descripcion: string | null;
  estado: string;
  creadoPor: string;
};

const colores: Record<string, string> = {
  "solicitud ingresada": "bg-emerald-100 text-emerald-900",
  "solicitud enviada a cliente": "bg-sky-100 text-sky-900",
  "solicitud en revision": "bg-amber-100 text-amber-900",
  "solicitud aprobada": "bg-teal-100 text-teal-900",
  "solicitud rechazada": "bg-rose-100 text-rose-900",
};

export function SolicitudesPage() {
  const { usuario, listo } = useSesion();
  const queryClient = useQueryClient();
  const [formulario, setFormulario] = useState(false);
  const [aviso, setAviso] = useState("");

  const plantillas = useQuery({
    queryKey: ["plantillas"],
    queryFn: () => api<string[]>("/api/departamento1/plantillas"),
    enabled: Boolean(usuario),
  });
  const sedes = useQuery({
    queryKey: ["sedes"],
    queryFn: () => api<string[]>("/api/departamento1/sedes"),
    enabled: Boolean(usuario),
  });
  const solicitudes = useQuery({
    queryKey: ["solicitudes"],
    queryFn: () => api<Solicitud[]>("/api/departamento1/solicitudes"),
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
    setAviso("La solicitud quedó registrada con estado solicitud ingresada.");
    await queryClient.invalidateQueries({ queryKey: ["solicitudes"] });
  }

  return (
    <div className="min-h-screen">
      <Encabezado />

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

        <div className="mt-5 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Empresa", "RUT", "Correo", "Teléfono", "Período", "Sede", "Plantilla", "Estado"].map((columna) => (
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
                  <td className="px-3 py-3">{solicitud.sedes.length ? solicitud.sedes.join(", ") : "Sin sede"}</td>
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
                  <td colSpan={8} className="px-3 py-8 text-center text-muted">
                    Aún no hay solicitudes.
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
          sedes={sedes.data ?? []}
          onCerrar={() => setFormulario(false)}
          onCreada={() => void creada()}
        />
      )}
    </div>
  );
}
