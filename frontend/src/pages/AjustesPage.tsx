import { useState } from "react";
import { api, ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";

const NOMBRE_OK = "El nombre quedó actualizado.";
const CLAVE_OK = "La contraseña quedó actualizada.";

export function AjustesPage() {
  const { usuario, actualizar } = useSesion();
  const [nombre, setNombre] = useState(usuario?.nombre ?? "");
  const [claveNombre, setClaveNombre] = useState("");
  const [claveActual, setClaveActual] = useState("");
  const [claveNueva, setClaveNueva] = useState("");
  const [nombreOk, setNombreOk] = useState("");
  const [claveNombreOk, setClaveNombreOk] = useState("");
  const [claveActualOk, setClaveActualOk] = useState("");
  const [claveNuevaOk, setClaveNuevaOk] = useState("");
  const [nombreError, setNombreError] = useState("");
  const [claveNombreError, setClaveNombreError] = useState("");
  const [claveActualError, setClaveActualError] = useState("");
  const [claveNuevaError, setClaveNuevaError] = useState("");

  async function guardarNombre() {
    setNombreOk("");
    setClaveNombreOk("");
    setNombreError("");
    setClaveNombreError("");
    try {
      const sesion = await api<NonNullable<typeof usuario>>("/api/cuenta/nombre", {
        method: "PUT",
        body: JSON.stringify({ nombre, clave: claveNombre }),
      });
      actualizar(sesion);
      setClaveNombre("");
      setNombreOk(NOMBRE_OK);
      setClaveNombreOk(NOMBRE_OK);
    } catch (err) {
      const campos = err instanceof ErrorApi ? err.campos : {};
      setNombreError(campos.nombre ?? "");
      setClaveNombreError(campos.clave ?? (err instanceof ErrorApi ? err.message : "No se pudo actualizar el nombre."));
      if (campos.nombre && !campos.clave) {
        setClaveNombreError("");
      }
    }
  }

  async function guardarClave() {
    setClaveActualOk("");
    setClaveNuevaOk("");
    setClaveActualError("");
    setClaveNuevaError("");
    try {
      const sesion = await api<NonNullable<typeof usuario>>("/api/cuenta/clave", {
        method: "PUT",
        body: JSON.stringify({ claveActual, claveNueva }),
      });
      actualizar(sesion);
      setClaveActual("");
      setClaveNueva("");
      setClaveActualOk(CLAVE_OK);
      setClaveNuevaOk(CLAVE_OK);
    } catch (err) {
      const campos = err instanceof ErrorApi ? err.campos : {};
      setClaveActualError(campos.claveActual ?? "");
      setClaveNuevaError(campos.claveNueva ?? "");
      if (!campos.claveActual && !campos.claveNueva) {
        setClaveNuevaError(err instanceof ErrorApi ? err.message : "No se pudo actualizar la contraseña.");
      }
    }
  }

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-xl px-4 py-6">
        <h1 className="font-serif text-3xl">Ajustes</h1>
        {usuario?.claveProvisoria && (
          <p className="mt-3 rounded-md bg-amber-100 px-3 py-2 text-sm text-amber-950">
            La contraseña actual es provisoria. Cámbiela desde este menú.
          </p>
        )}
        <section className="mt-5 rounded-xl border border-line bg-card p-4">
          <h2 className="font-semibold">Cambiar Nombre</h2>
          <label className="mt-3 block text-sm" htmlFor="nombre">
            Nombre que verán los demás departamentos
            <input
              id="nombre"
              value={nombre}
              maxLength={80}
              onChange={(evento) => setNombre(evento.target.value)}
              className="mt-1 w-full rounded-md border border-line px-3 py-2"
            />
          </label>
          <TextoCampo ok={nombreOk} error={nombreError} />
          <label className="mt-3 block text-sm" htmlFor="clave-nombre">
            Contraseña
            <input
              id="clave-nombre"
              type="password"
              value={claveNombre}
              onChange={(evento) => setClaveNombre(evento.target.value)}
              className="mt-1 w-full rounded-md border border-line px-3 py-2"
            />
          </label>
          <TextoCampo ok={claveNombreOk} error={claveNombreError} />
          <button type="button" className="mt-3 rounded-md bg-pine px-4 py-2 font-semibold text-white" onClick={() => void guardarNombre()}>
            Guardar nombre
          </button>
        </section>
        <section className="mt-4 rounded-xl border border-line bg-card p-4">
          <h2 className="font-semibold">Cambiar Contraseña</h2>
          <label className="mt-3 block text-sm" htmlFor="clave-actual">
            Contraseña actual
            <input
              id="clave-actual"
              type="password"
              value={claveActual}
              onChange={(evento) => setClaveActual(evento.target.value)}
              className="mt-1 w-full rounded-md border border-line px-3 py-2"
            />
          </label>
          <TextoCampo ok={claveActualOk} error={claveActualError} />
          <label className="mt-3 block text-sm" htmlFor="clave-nueva">
            Contraseña nueva
            <input
              id="clave-nueva"
              type="password"
              value={claveNueva}
              onChange={(evento) => setClaveNueva(evento.target.value)}
              className="mt-1 w-full rounded-md border border-line px-3 py-2"
            />
          </label>
          <TextoCampo ok={claveNuevaOk} error={claveNuevaError} />
          <p className="mt-1 text-xs text-muted">Entre 8 y 72 caracteres.</p>
          <button type="button" className="mt-3 rounded-md bg-pine px-4 py-2 font-semibold text-white" onClick={() => void guardarClave()}>
            Guardar contraseña
          </button>
        </section>
      </main>
    </div>
  );
}

function TextoCampo({ ok, error }: { ok: string; error: string }) {
  if (error) {
    return (
      <p className="texto-error-campo mt-1 text-sm" role="alert">
        {error}
      </p>
    );
  }
  if (ok) {
    return <p className="texto-aviso-campo mt-1 text-sm">{ok}</p>;
  }
  return null;
}
