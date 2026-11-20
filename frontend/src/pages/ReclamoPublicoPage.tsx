import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { api, ErrorApi } from "../api/client";
import { Calendario } from "../components/Calendario";
import { compararFechas, enmascararFecha, enmascararTelefono, fechaValida, type TipoTelefono } from "../domain/reglas";

const TIPOS = [
  "Calidad del hormigón",
  "Retraso en la entrega",
  "Atención del personal",
  "Facturación / documentación",
  "Otro",
] as const;

const HORARIOS = ["Mañana, 08:00 a 12:00", "Tarde, 12:00 a 17:00", "Cualquier horario"];

const campo =
  "mt-1 w-full rounded-lg border border-[#e6ddd2] bg-white px-3 py-2 text-sm outline-none focus:border-[#E2874E] focus:ring-2 focus:ring-[#E2874E]/25";

const MENSAJE_FECHA = "La fecha del servicio no puede ser posterior a hoy.";

function numeroReclamo(local: string, tipo: TipoTelefono): string {
  if (tipo === "celular" && /^\d{8}$/.test(local)) {
    return `569${local}`;
  }
  if (tipo === "fijo" && /^[2-8]\d{8}$/.test(local)) {
    return `56${local}`;
  }
  return "";
}

export function ReclamoPublicoPage() {
  const [nombre, setNombre] = useState("");
  const [tipoTelefono, setTipoTelefono] = useState<TipoTelefono>("celular");
  const [telefono, setTelefono] = useState("");
  const [obra, setObra] = useState("");
  const [fechaServicio, setFechaServicio] = useState("");
  const [calendario, setCalendario] = useState(false);
  const [anioCalendario, setAnioCalendario] = useState(() => new Date().getFullYear());
  const [mesCalendario, setMesCalendario] = useState(() => new Date().getMonth());
  const [empresa, setEmpresa] = useState("");
  const [correo, setCorreo] = useState("");
  const [ubicacion, setUbicacion] = useState("");
  const [guiaDespacho, setGuiaDespacho] = useState("");
  const [tipo, setTipo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [preferenciaContacto, setPreferenciaContacto] = useState("");
  const [horario, setHorario] = useState("");
  const [comentarios, setComentarios] = useState("");
  const [campos, setCampos] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [listo, setListo] = useState(false);

  useEffect(() => {
    void api("/api/auth/csrf").catch(() => undefined);
  }, []);

  function limpiar() {
    setNombre("");
    setTipoTelefono("celular");
    setTelefono("");
    setObra("");
    setFechaServicio("");
    setCalendario(false);
    setEmpresa("");
    setCorreo("");
    setUbicacion("");
    setGuiaDespacho("");
    setTipo("");
    setDescripcion("");
    setPreferenciaContacto("");
    setHorario("");
    setComentarios("");
    setCampos({});
    setError("");
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const telefonoCompleto = numeroReclamo(telefono, tipoTelefono);
    const errores: Record<string, string> = {};
    if (!telefonoCompleto) {
      errores.telefono = MENSAJE_TELEFONO;
    }
    if (fechaFutura(fechaServicio)) {
      errores.fechaServicio = MENSAJE_FECHA;
    }
    if (Object.keys(errores).length > 0) {
      setError("");
      setCampos(errores);
      return;
    }
    setEnviando(true);
    setError("");
    setCampos({});
    try {
      await api("/api/publico/reclamos", {
        method: "POST",
        body: JSON.stringify({
          nombre,
          telefono: telefonoCompleto,
          obra,
          fechaServicio,
          empresa,
          correo,
          ubicacion,
          guiaDespacho,
          tipo,
          descripcion,
          preferenciaContacto,
          horario,
          comentarios,
        }),
      });
      setListo(true);
    } catch (err) {
      if (err instanceof ErrorApi) {
        setError(err.message);
        setCampos(err.campos);
      } else {
        setError("No se pudo enviar el reclamo.");
      }
    } finally {
      setEnviando(false);
    }
  }

  if (listo) {
    return (
      <main className="flex min-h-screen items-center bg-[#F7F4EF] px-4">
        <section className="mx-auto max-w-xl rounded-2xl border border-[#e6ddd2] bg-white p-8 text-center shadow-sm">
          <img src="/logo-copat.png" alt="Copat" className="mx-auto h-16 w-auto" />
          <p className="mt-4 text-sm font-semibold text-[#4D8AC2]">D-SIG-09-01</p>
          <h1 className="mt-2 text-3xl font-semibold text-[#3F3A36]">Recibimos su reclamo</h1>
          <p className="mt-3 text-sm text-[#7A726A]">
            El equipo de Copat lo revisará y le responderá dentro de 24 horas hábiles, al teléfono o al correo que
            indicó.
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#F7F4EF] text-[#3F3A36]">
      <header className="border-b-4 border-[#E2874E] bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-4 px-4 py-4">
          <img src="/logo-copat.png" alt="Copat, 25 años cerca de ti" className="h-20 w-auto" />
          <div className="mr-auto sm:border-l sm:border-[#e6ddd2] sm:pl-6">
            <h1 className="text-xl font-semibold text-[#3F3A36] sm:text-2xl">Formulario de Reclamo de Clientes</h1>
          </div>
        </div>
      </header>

      <form noValidate onSubmit={enviar} className="mx-auto max-w-5xl px-4 py-6">
        <p className="rounded-xl border border-[#e6ddd2] bg-[#FFF8F1] px-4 py-3 text-sm text-[#5C534C]">
          En Copat valoramos tu opinión y nos comprometemos a atender su reclamo con seriedad. Complete este formulario
          para que nuestro equipo lo gestione. Le responderemos dentro de 24 horas hábiles.
        </p>

        <ol className="mt-5 grid gap-3 sm:grid-cols-3">
          {[
            ["1", "Datos del reclamo"],
            ["2", "Detalle"],
            ["3", "Contacto y envío"],
          ].map(([numero, etiqueta], indice) => (
            <li key={etiqueta} className="flex items-center gap-3 rounded-xl bg-white px-3 py-2 shadow-sm">
              <span
                className={`grid h-8 w-8 place-items-center rounded-full text-sm font-bold ${
                  indice === 0 ? "bg-[#E2874E] text-white" : "bg-[#F3EBE3] text-[#8A8178]"
                }`}
              >
                {numero}
              </span>
              <span className={indice === 0 ? "font-semibold" : "text-[#8A8178]"}>{etiqueta}</span>
            </li>
          ))}
        </ol>

        <section className="mt-4 rounded-2xl border border-[#e6ddd2] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">1. Datos generales</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <Campo etiqueta="Nombre completo *" error={campos.nombre}>
              <input className={campo} value={nombre} maxLength={120} placeholder="Ingrese su nombre" onChange={(evento) => setNombre(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Empresa / razón social *" error={campos.empresa}>
              <input className={campo} value={empresa} maxLength={150} placeholder="Ingrese la empresa" onChange={(evento) => setEmpresa(evento.target.value)} />
            </Campo>
            <fieldset>
              <legend className="text-sm font-semibold">Teléfono de contacto *</legend>
              <div className="mt-2 flex gap-2">
                {([
                  ["celular", "Celular"],
                  ["fijo", "Fijo"],
                ] as const).map(([opcion, etiqueta]) => {
                  const activo = tipoTelefono === opcion;
                  return (
                    <button
                      key={opcion}
                      type="button"
                      aria-pressed={activo}
                      onClick={() => {
                        setTipoTelefono(opcion);
                        setTelefono("");
                      }}
                      className={`rounded-lg border px-4 py-2 text-sm font-semibold ${
                        activo ? "border-[#4D8AC2] bg-[#E8F2FA] text-[#2F6A9E]" : "border-[#e6ddd2]"
                      }`}
                    >
                      {etiqueta}
                    </button>
                  );
                })}
              </div>
              <div className="mt-2 flex">
                <span className="inline-flex items-center rounded-l-lg border border-r-0 border-[#d5e4f2] bg-[#E8F2FA] px-3 text-sm font-semibold text-[#4D8AC2]">
                  {tipoTelefono === "celular" ? "+569" : "+56"}
                </span>
                <input
                  className="w-full rounded-r-lg border border-[#e6ddd2] bg-white px-3 py-2 text-sm outline-none focus:border-[#E2874E] focus:ring-2 focus:ring-[#E2874E]/25"
                  value={telefono}
                  inputMode="numeric"
                  autoComplete="tel-national"
                  aria-label="Teléfono sin prefijo"
                  placeholder={tipoTelefono === "celular" ? "12345678" : "221234567"}
                  onChange={(evento) => setTelefono(enmascararTelefono(evento.target.value, tipoTelefono))}
                />
              </div>
              {campos.telefono && <span className="mt-1 block text-xs font-normal text-[#7a1d1d]">{campos.telefono}</span>}
            </fieldset>
            <Campo etiqueta="Correo electrónico *" error={campos.correo}>
              <input className={campo} type="email" value={correo} placeholder="ejemplo@empresa.cl" onChange={(evento) => setCorreo(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Nombre de la obra *" error={campos.obra}>
              <input className={campo} value={obra} maxLength={200} placeholder="Ej: Edificio Mirador" onChange={(evento) => setObra(evento.target.value)} />
            </Campo>
            <Campo etiqueta="Ubicación (dirección, comuna, ciudad) *" error={campos.ubicacion}>
              <input className={campo} value={ubicacion} maxLength={200} placeholder="Ej: Av. Industrial 123, Antofagasta" onChange={(evento) => setUbicacion(evento.target.value)} />
            </Campo>
            <div className="relative">
              <label htmlFor="fecha-servicio" className="block text-sm font-semibold">
                Fecha del servicio *
              </label>
              <div className="mt-1 flex">
                <input
                  id="fecha-servicio"
                  className="w-full rounded-l-lg border border-[#e6ddd2] bg-white px-3 py-2 text-sm outline-none focus:border-[#E2874E] focus:ring-2 focus:ring-[#E2874E]/25"
                  value={fechaServicio}
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="dd/mm/aaaa"
                  onChange={(evento) => {
                    const siguiente = enmascararFecha(evento.target.value);
                    setFechaServicio(siguiente);
                    setCampos((actual) => {
                      const copia = { ...actual };
                      if (fechaFutura(siguiente)) {
                        copia.fechaServicio = MENSAJE_FECHA;
                      } else {
                        delete copia.fechaServicio;
                      }
                      return copia;
                    });
                  }}
                />
                <button
                  type="button"
                  className="rounded-r-lg border border-l-0 border-[#d5e4f2] bg-[#E8F2FA] px-3 text-[#4D8AC2]"
                  aria-label="Abrir calendario de fecha del servicio"
                  onClick={() => {
                    if (calendario) {
                      setCalendario(false);
                      return;
                    }
                    const actual = fechaValida(fechaServicio) && !fechaFutura(fechaServicio)
                      ? partirFecha(fechaServicio)
                      : { anio: new Date().getFullYear(), mes: new Date().getMonth() };
                    setAnioCalendario(actual.anio);
                    setMesCalendario(actual.mes);
                    setCalendario(true);
                  }}
                >
                  <CalendarioIcono />
                </button>
              </div>
              {calendario && (
                <Calendario
                  anio={anioCalendario}
                  mes={mesCalendario}
                  seleccionado={fechaServicio}
                  hasta={fechaDeHoy()}
                  acento="copat"
                  onCambiarMes={(anio, mes) => {
                    setAnioCalendario(anio);
                    setMesCalendario(mes);
                  }}
                  onElegir={(fecha) => {
                    setFechaServicio(fecha);
                    setCalendario(false);
                  }}
                  onCerrar={() => setCalendario(false)}
                />
              )}
              {campos.fechaServicio && <span className="mt-1 block text-xs font-normal text-[#7a1d1d]">{campos.fechaServicio}</span>}
            </div>
            <Campo etiqueta="Guía de despacho / orden (si aplica)" error={campos.guiaDespacho}>
              <input className={campo} value={guiaDespacho} maxLength={40} placeholder="Ej: GD-123456" onChange={(evento) => setGuiaDespacho(evento.target.value)} />
            </Campo>
          </div>
        </section>

        <section className="mt-4 rounded-2xl border border-[#e6ddd2] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">2. Tipo de reclamo</h2>
          <p className="mt-1 text-sm text-[#7A726A]">Seleccione el motivo de su reclamo *</p>
          <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
            {TIPOS.map((opcion) => {
              const activo = tipo === opcion;
              return (
                <button
                  key={opcion}
                  type="button"
                  aria-pressed={activo}
                  onClick={() => setTipo(opcion)}
                  className={`rounded-xl border px-3 py-3 text-left text-sm font-semibold ${
                    activo ? "border-[#4D8AC2] bg-[#E8F2FA] text-[#2F6A9E]" : "border-[#e6ddd2] bg-white"
                  }`}
                >
                  {opcion}
                </button>
              );
            })}
          </div>
          {campos.tipo && <p className="mt-2 text-xs text-[#7a1d1d]">{campos.tipo}</p>}
        </section>

        <section className="mt-4 rounded-2xl border border-[#e6ddd2] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">3. Descripción del reclamo</h2>
          <Campo etiqueta="Describa su reclamo *" error={campos.descripcion}>
            <textarea
              className={`${campo} resize-y`}
              rows={4}
              maxLength={1000}
              value={descripcion}
              placeholder="Explique en detalle su reclamo..."
              onChange={(evento) => setDescripcion(evento.target.value)}
            />
            <span className="mt-1 block text-right text-xs text-[#8A8178]">{descripcion.length} / 1.000</span>
          </Campo>
        </section>

        <section className="mt-4 rounded-2xl border border-[#e6ddd2] bg-white p-5 shadow-sm">
          <h2 className="font-semibold">4. Contacto y comentarios</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <fieldset>
              <legend className="text-sm font-semibold">¿Cómo prefiere que lo contactemos? *</legend>
              <div className="mt-2 flex gap-2">
                {["Teléfono", "Mail"].map((opcion) => {
                  const activo = preferenciaContacto === opcion;
                  return (
                    <button
                      key={opcion}
                      type="button"
                      aria-pressed={activo}
                      onClick={() => setPreferenciaContacto(opcion)}
                      className={`rounded-lg border px-4 py-2 text-sm font-semibold ${
                        activo ? "border-[#4D8AC2] bg-[#E8F2FA] text-[#2F6A9E]" : "border-[#e6ddd2]"
                      }`}
                    >
                      {opcion}
                    </button>
                  );
                })}
              </div>
              {campos.preferenciaContacto && <p className="mt-1 text-xs text-[#7a1d1d]">{campos.preferenciaContacto}</p>}
            </fieldset>
            <Campo etiqueta="Horario preferido para contacto" error={campos.horario}>
              <select className={campo} value={horario} onChange={(evento) => setHorario(evento.target.value)}>
                <option value="">Seleccione un horario</option>
                {HORARIOS.map((opcion) => (
                  <option key={opcion} value={opcion}>
                    {opcion}
                  </option>
                ))}
              </select>
            </Campo>
          </div>
          <div className="mt-4">
            <Campo etiqueta="Comentarios adicionales" error={campos.comentarios}>
              <textarea
                className={`${campo} resize-y`}
                rows={3}
                maxLength={500}
                value={comentarios}
                placeholder="Agregue comentarios o antecedentes que considere relevantes (opcional)..."
                onChange={(evento) => setComentarios(evento.target.value)}
              />
              <span className="mt-1 block text-right text-xs text-[#8A8178]">{comentarios.length} / 500</span>
            </Campo>
          </div>
        </section>

        {error && (
          <p className="mt-4 text-sm text-[#7a1d1d]" role="alert">
            {error}
          </p>
        )}

        <div className="mt-4 flex justify-end gap-3">
          <button type="button" onClick={limpiar} className="rounded-lg border border-[#e6ddd2] bg-white px-4 py-2 text-sm font-semibold">
            Cancelar
          </button>
          <button
            type="submit"
            disabled={enviando}
            className="rounded-lg bg-[#E2874E] px-5 py-2.5 font-semibold text-white disabled:opacity-60"
          >
            {enviando ? "Enviando…" : "Enviar reclamo"}
          </button>
        </div>
      </form>
    </main>
  );
}

function fechaDeHoy() {
  const hoy = new Date();
  const dia = String(hoy.getDate()).padStart(2, "0");
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  return `${dia}/${mes}/${hoy.getFullYear()}`;
}

function fechaFutura(valor: string) {
  return fechaValida(valor) && compararFechas(valor, fechaDeHoy()) > 0;
}

function partirFecha(fecha: string) {
  const [, mes, anio] = fecha.split("/").map(Number);
  return { anio, mes: mes - 1 };
}

function CalendarioIcono() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
      <path fill="currentColor" d="M7 2h2v2h6V2h2v2h3v18H4V4h3V2zm12 8H5v10h14V10zM5 8h14V6H5v2z" />
    </svg>
  );
}

function Campo({
  etiqueta,
  error,
  children,
}: {
  etiqueta: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold">
      {etiqueta}
      {children}
      {error && <span className="mt-1 block text-xs font-normal text-[#7a1d1d]">{error}</span>}
    </label>
  );
}

