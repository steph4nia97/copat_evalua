import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api, ErrorApi } from "../api/client";
import { Encabezado } from "../components/Encabezado";

type Usuario = {
  id: number;
  email: string;
  nombre: string;
  departamento: string;
  departamentoId: number;
  claveProvisoria: boolean;
};

type Registro = {
  id: number;
  fecha: string;
  hora: string;
  trabajador: string;
  departamento: string;
  accion: string;
  detalle: string;
};

const DEPARTAMENTOS = [
  { id: 1, nombre: "Departamento 1" },
  { id: 3, nombre: "Departamento 3" },
];

export function AdminPage() {
  const queryClient = useQueryClient();
  const [correo, setCorreo] = useState("");
  const [nombre, setNombre] = useState("");
  const [clave, setClave] = useState("");
  const [departamentoId, setDepartamentoId] = useState("1");
  const [filtro, setFiltro] = useState("");
  const [aviso, setAviso] = useState("");
  const [error, setError] = useState("");

  const usuarios = useQuery({
    queryKey: ["admin-usuarios"],
    queryFn: () => api<Usuario[]>("/api/admin/usuarios"),
  });
  const registros = useQuery({
    queryKey: ["admin-registros", filtro],
    queryFn: () => api<Registro[]>(`/api/admin/registros${filtro ? `?departamentoId=${filtro}` : ""}`),
  });

  async function crear() {
    setError("");
    setAviso("");
    try {
      await api("/api/admin/usuarios", {
        method: "POST",
        body: JSON.stringify({ correo, clave, nombre, departamentoId: Number(departamentoId) }),
      });
      setCorreo("");
      setNombre("");
      setClave("");
      setAviso("El usuario quedó creado con una clave provisoria.");
      await queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] });
      await queryClient.invalidateQueries({ queryKey: ["admin-registros"] });
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No se pudo crear el usuario.");
    }
  }

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <h1 className="font-serif text-3xl">Administración</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted">
          Cree las cuentas de cada departamento y revise los pasos registrados en todo el flujo.
        </p>

        <section className="mt-5 rounded-xl border border-line bg-card p-4">
          <h2 className="font-semibold">Nuevo usuario</h2>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <label className="text-sm" htmlFor="correo">
              Correo
              <input id="correo" value={correo} onChange={(evento) => setCorreo(evento.target.value)} className="mt-1 w-full rounded-md border border-line px-3 py-2" />
            </label>
            <label className="text-sm" htmlFor="nombre-usuario">
              Nombre
              <input id="nombre-usuario" value={nombre} maxLength={80} onChange={(evento) => setNombre(evento.target.value)} className="mt-1 w-full rounded-md border border-line px-3 py-2" />
            </label>
            <label className="text-sm" htmlFor="clave">
              Clave provisoria
              <input id="clave" type="password" value={clave} onChange={(evento) => setClave(evento.target.value)} className="mt-1 w-full rounded-md border border-line px-3 py-2" />
            </label>
            <label className="text-sm" htmlFor="departamento">
              Departamento
              <select id="departamento" value={departamentoId} onChange={(evento) => setDepartamentoId(evento.target.value)} className="mt-1 w-full rounded-md border border-line bg-card px-3 py-2">
                {DEPARTAMENTOS.map((departamento) => (
                  <option key={departamento.id} value={departamento.id}>{departamento.nombre}</option>
                ))}
              </select>
            </label>
          </div>
          <button type="button" className="mt-4 rounded-md bg-pine px-4 py-2 font-semibold text-white" onClick={() => void crear()}>
            Crear usuario
          </button>
          {error && <p className="mt-3 text-sm text-copper-dark" role="alert">{error}</p>}
          {aviso && <p className="mt-3 text-sm text-pine">{aviso}</p>}
        </section>

        <section className="mt-5 overflow-x-auto rounded-xl border border-line bg-card">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Nombre", "Correo", "Departamento", "Clave"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">{columna}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(usuarios.data ?? []).map((usuario) => (
                <tr key={usuario.id} className="border-t border-line">
                  <td className="px-3 py-2">{usuario.nombre}</td>
                  <td className="px-3 py-2">{usuario.email}</td>
                  <td className="px-3 py-2">{usuario.departamento}</td>
                  <td className="px-3 py-2">{usuario.claveProvisoria ? "Provisoria" : "Definida por el usuario"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="mt-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-serif text-2xl">Registro de actividad</h2>
            <label className="text-sm" htmlFor="filtro-log">
              Departamento
              <select id="filtro-log" value={filtro} onChange={(evento) => setFiltro(evento.target.value)} className="ml-2 rounded-md border border-line bg-card px-3 py-2">
                <option value="">Todos</option>
                <option value="4">Administración</option>
                {DEPARTAMENTOS.map((departamento) => (
                  <option key={departamento.id} value={departamento.id}>{departamento.nombre}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="mt-3 overflow-x-auto rounded-xl border border-line bg-card">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-paper text-muted">
                <tr>
                  {["Cuándo", "Trabajador", "Departamento", "Acción", "Detalle"].map((columna) => (
                    <th key={columna} className="px-3 py-2 font-semibold">{columna}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {(registros.data ?? []).map((registro) => (
                  <tr key={registro.id} className="border-t border-line align-top">
                    <td className="whitespace-nowrap px-3 py-2">{registro.fecha} {registro.hora}</td>
                    <td className="px-3 py-2">{registro.trabajador}</td>
                    <td className="px-3 py-2">{registro.departamento}</td>
                    <td className="px-3 py-2">{registro.accion}</td>
                    <td className="px-3 py-2">{registro.detalle}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}
