import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { ErrorApi } from "../api/client";
import { useSesion } from "../auth/sesion";
import { mensajes } from "../domain/reglas";

export function LoginPage() {
  const { usuario, listo, ingresar } = useSesion();
  const [correo, setCorreo] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);

  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (usuario) {
    return <Navigate to="/" replace />;
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setError("");
    try {
      await ingresar(correo, contrasena);
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : mensajes.credenciales);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-4">
      <form noValidate onSubmit={enviar} className="w-full rounded-xl border border-line bg-card p-6 shadow-card">
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-copper">Departamento 1</p>
        <h1 className="mt-2 font-serif text-4xl text-pine">Copat evalua</h1>
        <p className="mt-2 text-sm text-muted">Ingrese con su correo y contraseña.</p>
        <label htmlFor="correo" className="mt-6 block text-sm font-semibold">
          Correo
        </label>
        <input
          id="correo"
          type="email"
          autoComplete="username"
          value={correo}
          onChange={(evento) => setCorreo(evento.target.value)}
          className="mt-1 w-full rounded-md border border-line px-3 py-2 outline-none ring-copper focus:ring-2"
        />
        <label htmlFor="contrasena" className="mt-4 block text-sm font-semibold">
          Contraseña
        </label>
        <input
          id="contrasena"
          type="password"
          autoComplete="current-password"
          value={contrasena}
          onChange={(evento) => setContrasena(evento.target.value)}
          className="mt-1 w-full rounded-md border border-line px-3 py-2 outline-none ring-copper focus:ring-2"
        />
        {error && (
          <p className="mt-4 text-sm text-copper-dark" role="alert">
            {error}
          </p>
        )}
        <button
          type="submit"
          disabled={enviando}
          className="mt-6 w-full rounded-md bg-pine px-4 py-2 font-semibold text-white hover:bg-pine-deep disabled:opacity-60"
        >
          Ingresar
        </button>
      </form>
    </main>
  );
}
