import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { api, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";

type Usuario = {
  id: number;
  email: string;
  nombre: string;
  cargo: string;
  departamento: string;
  departamentoId: number;
  claveProvisoria: boolean;
  activo: boolean;
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
  const [cargo, setCargo] = useState("");
  const [departamentoId, setDepartamentoId] = useState("1");
  const [filtro, setFiltro] = useState("");
  const [aviso, setAviso] = useState("");
  const [error, setError] = useState("");
  const [eliminado, setEliminado] = useState("");

  useEffect(() => {
    if (!eliminado) {
      return;
    }
    const reloj = window.setTimeout(() => setEliminado(""), 3000);
    return () => window.clearTimeout(reloj);
  }, [eliminado]);

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
        body: JSON.stringify({ correo, clave, nombre, cargo, departamentoId: Number(departamentoId) }),
      });
      setCorreo("");
      setNombre("");
      setClave("");
      setCargo("");
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
          Cree, edite o elimine las cuentas de cada departamento y revise los pasos registrados en todo el flujo.
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
            <label className="text-sm" htmlFor="cargo">
              Cargo
              <input id="cargo" value={cargo} maxLength={80} onChange={(evento) => setCargo(evento.target.value)} className="mt-1 w-full rounded-md border border-line px-3 py-2" />
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
                {["Nombre", "Correo", "Cargo", "Departamento", "Clave", ""].map((columna) => (
                  <th key={columna || "acciones"} className="px-3 py-2 font-semibold">{columna}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {eliminado && (
                <tr>
                  <td colSpan={6} className="px-3 pt-2 text-xs text-pine">{eliminado}</td>
                </tr>
              )}
              {(usuarios.data ?? []).map((usuario) => (
                <FilaUsuario
                  key={usuario.id}
                  usuario={usuario}
                  onGuardado={() => {
                    void queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] });
                    void queryClient.invalidateQueries({ queryKey: ["admin-registros"] });
                  }}
                  onEliminado={() => {
                    setEliminado("Usuario eliminado");
                    void queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] });
                    void queryClient.invalidateQueries({ queryKey: ["admin-registros"] });
                  }}
                />
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

function FilaUsuario({
  usuario,
  onGuardado,
  onEliminado,
}: {
  usuario: Usuario;
  onGuardado: () => void;
  onEliminado: () => void;
}) {
  const { usuario: sesion, actualizar } = useSesion();
  const propia = sesion?.email === usuario.email;
  const [nombre, setNombre] = useState(usuario.nombre);
  const [correo, setCorreo] = useState(usuario.email);
  const [cargo, setCargo] = useState(usuario.cargo);
  const [departamentoId, setDepartamentoId] = useState(String(usuario.departamentoId));
  const [clave, setClave] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [confirmar, setConfirmar] = useState(false);
  const [desactivarPaso, setDesactivarPaso] = useState(false);
  const [ticket, setTicket] = useState<"" | "ok" | "error" | "error-eliminar">("");
  const [avisoOk, setAvisoOk] = useState("Guardado exitoso");
  const [detalle, setDetalle] = useState("");
  const ocupado = guardando || eliminando;

  useEffect(() => {
    setNombre(usuario.nombre);
    setCorreo(usuario.email);
    setCargo(usuario.cargo);
    setDepartamentoId(String(usuario.departamentoId));
    setClave("");
  }, [usuario.nombre, usuario.email, usuario.cargo, usuario.departamentoId]);

  useEffect(() => {
    if (ticket !== "ok") {
      return;
    }
    const reloj = window.setTimeout(() => setTicket(""), 3000);
    return () => window.clearTimeout(reloj);
  }, [ticket]);

  function limpiarAviso() {
    setTicket("");
    setDetalle("");
    setAvisoOk("Guardado exitoso");
  }

  async function guardar() {
    limpiarAviso();
    setGuardando(true);
    try {
      await api(`/api/admin/usuarios/${usuario.id}`, {
        method: "PUT",
        body: JSON.stringify({
          correo,
          nombre,
          cargo,
          departamentoId: Number(departamentoId),
          clave,
        }),
      });
      onGuardado();
      if (propia) {
        actualizar(await api("/api/auth/me"));
      }
      setClave("");
      setAvisoOk("Guardado exitoso");
      setTicket("ok");
    } catch (err) {
      const campos = err instanceof ErrorApi ? err.campos : {};
      setDetalle(Object.values(campos)[0] ?? "");
      setTicket("error");
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar() {
    limpiarAviso();
    setEliminando(true);
    try {
      await api(`/api/admin/usuarios/${usuario.id}`, { method: "DELETE" });
      onEliminado();
    } catch (err) {
      const mensaje = err instanceof ErrorApi ? err.message : "Error al eliminar";
      if (mensaje.includes("movimientos registrados")) {
        setConfirmar(false);
        setDesactivarPaso(true);
        return;
      }
      setDetalle(mensaje);
      setTicket("error-eliminar");
      setConfirmar(false);
    } finally {
      setEliminando(false);
    }
  }

  async function cambiarAcceso(activar: boolean) {
    limpiarAviso();
    setEliminando(true);
    try {
      await api(`/api/admin/usuarios/${usuario.id}/${activar ? "reactivar" : "desactivar"}`, { method: "POST" });
      onGuardado();
      setDesactivarPaso(false);
      setAvisoOk(activar ? "Usuario reactivado" : "Usuario desactivado");
      setTicket("ok");
    } catch (err) {
      setDetalle(err instanceof ErrorApi ? err.message : "Error al guardar");
      setTicket("error-eliminar");
    } finally {
      setEliminando(false);
    }
  }

  const campo = "w-full min-w-32 rounded-md border border-line px-2 py-1 disabled:opacity-60";
  const guardarClase = `rounded-md px-3 py-1 text-xs font-semibold text-paper active:translate-y-px disabled:cursor-wait ${
    guardando ? "bg-pine-deep" : "bg-pine"
  }`;
  const eliminarClase = "rounded-md bg-copper px-3 py-1 text-xs font-semibold text-paper active:translate-y-px disabled:cursor-wait";
  const confirmarClase = `rounded-md px-3 py-1 text-xs font-semibold text-paper active:translate-y-px disabled:cursor-wait ${
    eliminando ? "bg-copper" : "bg-copper-dark"
  }`;
  const cancelarClase = "rounded-md border border-line bg-paper px-3 py-1 text-xs text-muted active:translate-y-px disabled:cursor-wait";
  const reactivarClase = "rounded-md border border-pine bg-pine-soft px-3 py-1 text-xs font-semibold text-pine active:translate-y-px disabled:cursor-wait";

  return (
    <tr className="border-t border-line align-top">
      <td className="px-3 py-2">
        <input aria-label={`Nombre de ${usuario.nombre}`} value={nombre} maxLength={80} disabled={ocupado} onChange={(evento) => { setNombre(evento.target.value); limpiarAviso(); }} className={campo} />
        {!usuario.activo && <p className="mt-1 text-xs text-muted">usuario desactivado</p>}
      </td>
      <td className="px-3 py-2">
        <input aria-label={`Correo de ${usuario.nombre}`} value={correo} disabled={ocupado} onChange={(evento) => { setCorreo(evento.target.value); limpiarAviso(); }} className={campo} />
      </td>
      <td className="px-3 py-2">
        <input aria-label={`Cargo de ${usuario.nombre}`} value={cargo} maxLength={80} disabled={ocupado} onChange={(evento) => { setCargo(evento.target.value); limpiarAviso(); }} className={campo} />
      </td>
      <td className="px-3 py-2">
        {usuario.departamentoId === 4 ? (
          <span>{usuario.departamento}</span>
        ) : (
          <select
            aria-label={`Departamento de ${usuario.nombre}`}
            value={departamentoId}
            disabled={ocupado}
            onChange={(evento) => { setDepartamentoId(evento.target.value); limpiarAviso(); }}
            className={campo}
          >
            {DEPARTAMENTOS.map((departamento) => (
              <option key={departamento.id} value={departamento.id}>{departamento.nombre}</option>
            ))}
          </select>
        )}
      </td>
      <td className="px-3 py-2">
        <input
          aria-label={`Nueva clave de ${usuario.nombre}`}
          type="password"
          autoComplete="new-password"
          placeholder="Nueva clave"
          value={clave}
          disabled={ocupado}
          onChange={(evento) => { setClave(evento.target.value); limpiarAviso(); }}
          className={campo}
        />
      </td>
      <td className="whitespace-nowrap px-3 py-2">
        <div className="flex flex-nowrap items-center gap-2">
          <button type="button" disabled={ocupado} aria-busy={guardando} className={guardarClase} onClick={() => void guardar()}>
            {guardando ? "Guardando…" : "Guardar"}
          </button>
          {!propia && usuario.activo && !confirmar && !desactivarPaso && (
            <button type="button" disabled={ocupado} className={eliminarClase} onClick={() => { limpiarAviso(); setConfirmar(true); }}>
              Eliminar
            </button>
          )}
          {!propia && !usuario.activo && (
            <button type="button" disabled={ocupado} aria-busy={eliminando} className={reactivarClase} onClick={() => void cambiarAcceso(true)}>
              {eliminando ? "Reactivando…" : "Reactivar"}
            </button>
          )}
          {!propia && confirmar && (
            <>
              <button type="button" disabled={ocupado} aria-busy={eliminando} className={confirmarClase} onClick={() => void eliminar()}>
                {eliminando ? "Eliminando…" : "Confirmar"}
              </button>
              <button type="button" disabled={ocupado} className={cancelarClase} onClick={() => setConfirmar(false)}>
                Cancelar
              </button>
            </>
          )}
          {!propia && desactivarPaso && (
            <>
              <button type="button" disabled={ocupado} aria-busy={eliminando} className={confirmarClase} onClick={() => void cambiarAcceso(false)}>
                {eliminando ? "Desactivando…" : "Desactivar"}
              </button>
              <button type="button" disabled={ocupado} className={cancelarClase} onClick={() => setDesactivarPaso(false)}>
                Cancelar
              </button>
            </>
          )}
        </div>
        {desactivarPaso && (
          <p className="mt-1 max-w-xs whitespace-normal text-xs text-ink">
            No se borra lo que hizo. En el historial quedará como {usuario.nombre} (usuario desactivado).
          </p>
        )}
        {ticket === "ok" && <p role="status" className="mt-1 whitespace-normal text-xs text-pine">{avisoOk}</p>}
        {ticket === "error" && (
          <p role="alert" className="mt-2 inline-flex rounded-md border border-copper/30 bg-copper/10 px-2 py-1 text-xs font-semibold text-copper-dark">
            Error al guardar
          </p>
        )}
        {ticket === "error" && detalle && <p className="mt-1 text-xs text-copper-dark">{detalle}</p>}
        {ticket === "error-eliminar" && (
          <p role="alert" className="mt-2 inline-flex rounded-md border border-copper/30 bg-copper/10 px-2 py-1 text-xs font-semibold text-copper-dark">
            {detalle || "Error al eliminar"}
          </p>
        )}
      </td>
    </tr>
  );
}
