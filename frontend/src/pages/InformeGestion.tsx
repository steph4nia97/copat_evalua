import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { api, descargar, ErrorApi, obtenerBlob } from "../api/client";
import { useSesion } from "../auth/sesion";
import { CampoFecha } from "../components/CampoFecha";
import { compararFechas, fechaValida } from "../domain/reglas";

type Corte = { nombre: string; cantidad: number; porcentaje: string };

type Gestion = {
  desde: string;
  hasta: string;
  total: number;
  porTipo: Corte[];
  porArea: Corte[];
  reincidentes: number;
  porcentajeReincidentes: string;
  estadoReincidentes: string;
  cerradosEnPlazo: number;
  porcentajeCierre: string;
  estadoCierre: string;
  tiempoPromedio: string;
  estadoTiempo: string;
  principalesCausas: string;
  cumplimientoPlazos: string;
  responsable: string | null;
  cargo: string | null;
  aprobadoPor: string | null;
  causasRecurrentes: string | null;
  areasFrecuencia: string | null;
  efectividad: string | null;
  conclusiones: string | null;
  comercialNombre: string | null;
  comercialCargo: string | null;
  comercialUsuarioId: number | null;
  comercialFecha: string | null;
  comercialFirmaVirtual: boolean;
  gerenciaNombre: string | null;
  gerenciaCargo: string | null;
  gerenciaUsuarioId: number | null;
  gerenciaFecha: string | null;
  gerenciaFirmaVirtual: boolean;
  comercialTieneFirma: boolean;
  gerenciaTieneFirma: boolean;
};

type Firmante = { id: number; nombre: string; cargo: string; departamento: string };

const campo = "mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm";

function textoError(err: unknown, respaldo: string) {
  if (!(err instanceof ErrorApi)) return respaldo;
  return Object.values(err.campos)[0] || err.message;
}

export function InformeGestion() {
  const { usuario } = useSesion();
  const queryClient = useQueryClient();
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [responsable, setResponsable] = useState("");
  const [cargo, setCargo] = useState("");
  const [aprobadoPor, setAprobadoPor] = useState("");
  const [efectividad, setEfectividad] = useState("");
  const [conclusiones, setConclusiones] = useState("");
  const [comercialId, setComercialId] = useState("");
  const [comercialFecha, setComercialFecha] = useState("");
  const [comercialVirtual, setComercialVirtual] = useState(false);
  const [gerenciaId, setGerenciaId] = useState("");
  const [gerenciaFecha, setGerenciaFecha] = useState("");
  const [gerenciaVirtual, setGerenciaVirtual] = useState(false);
  const [comercialArchivo, setComercialArchivo] = useState<File | null>(null);
  const [gerenciaArchivo, setGerenciaArchivo] = useState<File | null>(null);
  const [comercialQuitar, setComercialQuitar] = useState(false);
  const [gerenciaQuitar, setGerenciaQuitar] = useState(false);
  const [comercialVista, setComercialVista] = useState("");
  const [gerenciaVista, setGerenciaVista] = useState("");
  const [comercialClave, setComercialClave] = useState(0);
  const [gerenciaClave, setGerenciaClave] = useState(0);
  const firmantes = useQuery({
    queryKey: ["firmantes"],
    queryFn: () => api<Firmante[]>("/api/departamento3/reclamos/firmantes"),
  });
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState("");
  const [guardando, setGuardando] = useState(false);
  const rangoInvalido = fechaValida(desde) && fechaValida(hasta) && compararFechas(desde, hasta) > 0;
  const listo = fechaValida(desde) && fechaValida(hasta) && !rangoInvalido;
  const gestion = useQuery({
    queryKey: ["gestion-reclamos", desde, hasta],
    queryFn: () => api<Gestion>(`/api/departamento3/reclamos/gestion?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}`),
    enabled: listo,
  });

  useEffect(() => {
    const datos = gestion.data;
    if (!datos) return;
    setResponsable(datos.responsable ?? usuario?.nombre ?? "");
    setCargo(datos.cargo ?? usuario?.cargo ?? "");
    setAprobadoPor(datos.aprobadoPor ?? "");
    setEfectividad(datos.efectividad ?? "");
    setConclusiones(datos.conclusiones ?? "");
    setComercialId(datos.comercialUsuarioId ? String(datos.comercialUsuarioId) : "");
    setComercialFecha(datos.comercialFecha ?? "");
    setComercialVirtual(datos.comercialFirmaVirtual);
    setGerenciaId(datos.gerenciaUsuarioId ? String(datos.gerenciaUsuarioId) : "");
    setGerenciaFecha(datos.gerenciaFecha ?? "");
    setGerenciaVirtual(datos.gerenciaFirmaVirtual);
  }, [gestion.data, usuario]);

  useEffect(() => {
    setComercialArchivo(null);
    setGerenciaArchivo(null);
    setComercialQuitar(false);
    setGerenciaQuitar(false);
  }, [desde, hasta]);

  useEffect(() => {
    let activo = true;
    const urls: string[] = [];
    async function preparar(
      rol: "comercial" | "gerencia",
      tiene: boolean,
      local: File | null,
      quitar: boolean,
      publicar: (valor: string) => void,
    ) {
      if (local) {
        const url = URL.createObjectURL(local);
        if (!activo) {
          URL.revokeObjectURL(url);
          return;
        }
        urls.push(url);
        publicar(url);
        return;
      }
      if (quitar || !tiene) {
        if (activo) publicar("");
        return;
      }
      try {
        const blob = await obtenerBlob(
          `/api/departamento3/reclamos/gestion/firma?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}&rol=${rol}`,
        );
        const url = URL.createObjectURL(blob);
        if (!activo) {
          URL.revokeObjectURL(url);
          return;
        }
        urls.push(url);
        publicar(url);
      } catch {
        if (activo) publicar("");
      }
    }
    void preparar("comercial", Boolean(gestion.data?.comercialTieneFirma), comercialArchivo, comercialQuitar, setComercialVista);
    void preparar("gerencia", Boolean(gestion.data?.gerenciaTieneFirma), gerenciaArchivo, gerenciaQuitar, setGerenciaVista);
    return () => {
      activo = false;
      for (const url of urls) URL.revokeObjectURL(url);
    };
  }, [gestion.data, desde, hasta, comercialArchivo, gerenciaArchivo, comercialQuitar, gerenciaQuitar]);

  function elegirFirma(archivo: File | null, asignar: (valor: File | null) => void, aceptar: () => void, reiniciar: () => void) {
    setError("");
    if (!archivo) {
      asignar(null);
      return;
    }
    const tipo = archivo.type;
    const nombre = archivo.name.toLowerCase();
    const valido = tipo === "image/png" || tipo === "image/jpeg" || nombre.endsWith(".png") || nombre.endsWith(".jpg") || nombre.endsWith(".jpeg");
    if (!valido || archivo.size > 1_000_000) {
      setError("La firma debe ser una imagen PNG o JPG de hasta 1 MB.");
      reiniciar();
      return;
    }
    aceptar();
    asignar(archivo);
  }

  async function subirFirma(rol: "comercial" | "gerencia", archivo: File) {
    const cuerpo = new FormData();
    cuerpo.set("archivo", archivo);
    await api(
      `/api/departamento3/reclamos/gestion/firma?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}&rol=${rol}`,
      { method: "POST", body: cuerpo },
    );
  }

  async function guardar() {
    setGuardando(true);
    setError("");
    setAviso("");
    try {
      await api("/api/departamento3/reclamos/gestion", {
        method: "PUT",
        body: JSON.stringify({
          desde,
          hasta,
          responsable,
          cargo,
          aprobadoPor,
          efectividad,
          conclusiones,
          comercialUsuarioId: comercialId ? Number(comercialId) : null,
          comercialFecha,
          comercialFirmaVirtual: Boolean(comercialId) && comercialVirtual,
          comercialQuitarFirma: comercialQuitar || !comercialVirtual,
          gerenciaUsuarioId: gerenciaId ? Number(gerenciaId) : null,
          gerenciaFecha,
          gerenciaFirmaVirtual: Boolean(gerenciaId) && gerenciaVirtual,
          gerenciaQuitarFirma: gerenciaQuitar || !gerenciaVirtual,
        }),
      });
      if (comercialId && comercialVirtual && comercialArchivo) {
        await subirFirma("comercial", comercialArchivo);
      }
      if (gerenciaId && gerenciaVirtual && gerenciaArchivo) {
        await subirFirma("gerencia", gerenciaArchivo);
      }
      setAviso("Informe guardado.");
      setComercialArchivo(null);
      setGerenciaArchivo(null);
      setComercialQuitar(false);
      setGerenciaQuitar(false);
      setComercialClave((valor) => valor + 1);
      setGerenciaClave((valor) => valor + 1);
      await queryClient.invalidateQueries({ queryKey: ["gestion-reclamos", desde, hasta] });
    } catch (err) {
      setError(textoError(err, "No se pudo guardar el informe."));
    } finally {
      setGuardando(false);
    }
  }

  async function bajar() {
    setError("");
    try {
      await descargar(
        `/api/departamento3/reclamos/gestion/informe?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}`,
        `D-SIG-09-03-${desde.replaceAll("/", "-")}_${hasta.replaceAll("/", "-")}.pdf`,
      );
    } catch (err) {
      setError(textoError(err, "No se pudo descargar el informe."));
    }
  }

  const datos = gestion.data;

  return (
    <section className="mt-5">
      <h2 className="font-serif text-2xl">Informe de Gestión de Reclamos</h2>
      <p className="mt-1 max-w-3xl text-sm text-muted">
        D-SIG-09-03. Los porcentajes salen de los reclamos recibidos en el periodo. El seguimiento de cada caso queda en la otra vista.
      </p>
      <div className="mt-4 grid gap-3 rounded-xl border border-line bg-card p-4 sm:grid-cols-2">
        <CampoFecha id="gestion-desde" label="Periodo desde" value={desde} onChange={setDesde} onBlur={() => undefined} error={desde && !fechaValida(desde) ? "Use el formato dd/mm/aaaa." : undefined} />
        <CampoFecha id="gestion-hasta" label="Periodo hasta" value={hasta} onChange={setHasta} onBlur={() => undefined} error={rangoInvalido ? "La fecha final es anterior a la inicial." : hasta && !fechaValida(hasta) ? "Use el formato dd/mm/aaaa." : undefined} />
      </div>

      {!listo && <p className="mt-4 text-sm text-muted">Indique el periodo. Después se abren los campos del informe.</p>}
      {gestion.isError && (
        <p className="mt-4 text-sm text-copper-dark" role="alert">
          {gestion.error instanceof ErrorApi ? gestion.error.message : "No se pudo calcular el periodo."}
        </p>
      )}

      {datos && (
        <>
          <section className="mt-4 grid gap-3 rounded-xl border border-line bg-card p-4 sm:grid-cols-2">
            <label className="text-sm font-semibold">
              Responsable de evaluación
              <input className={campo} value={responsable} maxLength={80} onChange={(evento) => setResponsable(evento.target.value)} />
            </label>
            <label className="text-sm font-semibold">
              Cargo
              <input className={campo} value={cargo} maxLength={80} onChange={(evento) => setCargo(evento.target.value)} />
            </label>
            <label className="text-sm font-semibold sm:col-span-2">
              Aprobado por
              <input className={campo} value={aprobadoPor} maxLength={80} onChange={(evento) => setAprobadoPor(evento.target.value)} />
            </label>
          </section>

          <section className="mt-4 rounded-xl border border-line bg-card p-4">
            <h3 className="font-semibold">Resumen ejecutivo</h3>
            <p className="mt-2 font-serif text-3xl">{datos.total}</p>
            <p className="text-sm text-muted">Número total de reclamos</p>
            <h4 className="mt-4 text-sm font-semibold">Principales causas</h4>
            <p className="mt-1 whitespace-pre-line text-sm">{datos.principalesCausas}</p>
            <h4 className="mt-4 text-sm font-semibold">Cumplimiento de plazos</h4>
            <p className="mt-1 text-sm">{datos.cumplimientoPlazos}</p>
          </section>

          <div className="mt-4 grid gap-4 lg:grid-cols-3">
            <article className="rounded-xl border border-line bg-card p-4">
              <h3 className="font-semibold">% de reclamos por tipo y área</h3>
              <Lista titulo="Tipo" items={datos.porTipo} />
              <Lista titulo="Área" items={datos.porArea} />
            </article>
            <article className="rounded-xl border border-line bg-card p-4">
              <h3 className="font-semibold">Tiempo promedio de cierre</h3>
              <p className="mt-2 font-serif text-3xl">{datos.tiempoPromedio === "—" ? "—" : `${datos.tiempoPromedio} días`}</p>
              <p className="mt-1 text-sm text-muted">Promedio de los reclamos cerrados del periodo. Meta: 5 días o menos.</p>
              <Estado valor={datos.estadoTiempo} />
            </article>
            <article className="rounded-xl border border-line bg-card p-4">
              <h3 className="font-semibold">Reclamos reincidentes</h3>
              <p className="mt-2 font-serif text-3xl">{datos.reincidentes}</p>
              <p className="text-sm text-muted">{datos.porcentajeReincidentes}% del periodo</p>
              <p className="mt-2 text-sm text-muted">Un reclamo es reincidente cuando la misma empresa ya tiene otro del mismo tipo dentro del periodo.</p>
              <Estado valor={datos.estadoReincidentes} />
            </article>
          </div>

          <section className="mt-4 overflow-x-auto rounded-xl border border-line bg-card">
            <h3 className="px-4 pt-4 font-semibold">Seguimiento de indicadores</h3>
            <table className="mt-2 w-full min-w-[640px] text-left text-sm">
              <thead className="border-b border-line text-xs uppercase tracking-wide text-muted">
                <tr>
                  <th className="px-4 py-2 font-semibold">Indicador</th>
                  <th className="px-4 py-2 font-semibold">Meta</th>
                  <th className="px-4 py-2 font-semibold">Fórmula</th>
                  <th className="px-4 py-2 font-semibold">Resultado</th>
                  <th className="px-4 py-2 font-semibold">Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-t border-line">
                  <td className="px-4 py-3">% de reclamos cerrados ≤ 5 días</td>
                  <td className="px-4 py-3">≥ 90%</td>
                  <td className="px-4 py-3">Cerrados en 5 días o menos / total × 100</td>
                  <td className="px-4 py-3">{datos.porcentajeCierre}%</td>
                  <td className="px-4 py-3">{datos.estadoCierre}</td>
                </tr>
                <tr className="border-t border-line">
                  <td className="px-4 py-3">Tiempo promedio de cierre</td>
                  <td className="px-4 py-3">≤ 5 días</td>
                  <td className="px-4 py-3">Suma de días de cierre / reclamos cerrados</td>
                  <td className="px-4 py-3">{datos.tiempoPromedio} días</td>
                  <td className="px-4 py-3">{datos.estadoTiempo}</td>
                </tr>
                <tr className="border-t border-line">
                  <td className="px-4 py-3">% de reclamos reincidentes</td>
                  <td className="px-4 py-3">≤ 5%</td>
                  <td className="px-4 py-3">Reincidentes / total × 100</td>
                  <td className="px-4 py-3">{datos.porcentajeReincidentes}%</td>
                  <td className="px-4 py-3">{datos.estadoReincidentes}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section className="mt-4 rounded-xl border border-line bg-card p-4">
            <h3 className="font-semibold">Análisis y evaluación de reclamos</h3>
            <h4 className="mt-3 text-sm font-semibold">Causas recurrentes</h4>
            <p className="mt-1 whitespace-pre-line text-sm">{datos.causasRecurrentes}</p>
            <h4 className="mt-4 text-sm font-semibold">Áreas con mayor frecuencia de reclamos</h4>
            <p className="mt-1 whitespace-pre-line text-sm">{datos.areasFrecuencia}</p>
            <label className="mt-3 block text-sm font-semibold">
              Efectividad de acciones correctivas
              <textarea className={campo} rows={3} maxLength={1000} value={efectividad} onChange={(evento) => setEfectividad(evento.target.value)} />
            </label>
          </section>

          <section className="mt-4 rounded-xl border border-line bg-card p-4">
            <h3 className="font-semibold">Conclusiones y acciones de mejora</h3>
            <textarea className={`${campo} mt-3`} rows={4} maxLength={2000} value={conclusiones} onChange={(evento) => setConclusiones(evento.target.value)} aria-label="Conclusiones y acciones de mejora" />
          </section>

          <section className="mt-4 rounded-xl border border-line bg-card p-4">
            <h3 className="font-semibold">Firma y aprobación</h3>
            <p className="mt-1 text-sm text-muted">La persona se elige según su perfil. La firma virtual es opcional: puede subir una imagen PNG o JPG y queda impresa en el informe.</p>
            <div className="mt-3 grid gap-4 lg:grid-cols-2">
              <Firma
                rol="Encargado área comercial"
                personas={firmantes.data ?? []}
                personaId={comercialId}
                fecha={comercialFecha}
                virtual={comercialVirtual}
                vista={comercialVista}
                clave={comercialClave}
                onPersona={(valor) => {
                  setComercialId(valor);
                  setComercialArchivo(null);
                  setComercialQuitar(true);
                  setComercialClave((actual) => actual + 1);
                  if (!valor) setComercialVirtual(false);
                }}
                onFecha={setComercialFecha}
                onVirtual={(valor) => {
                  setComercialVirtual(valor);
                  if (!valor) {
                    setComercialArchivo(null);
                    setComercialClave((actual) => actual + 1);
                  }
                }}
                onArchivo={(archivo) => elegirFirma(
                  archivo,
                  setComercialArchivo,
                  () => setComercialQuitar(false),
                  () => setComercialClave((actual) => actual + 1),
                )}
                onQuitar={() => {
                  setComercialArchivo(null);
                  setComercialQuitar(true);
                  setComercialClave((actual) => actual + 1);
                }}
              />
              <Firma
                rol="Gerencia Comercial"
                personas={firmantes.data ?? []}
                personaId={gerenciaId}
                fecha={gerenciaFecha}
                virtual={gerenciaVirtual}
                vista={gerenciaVista}
                clave={gerenciaClave}
                onPersona={(valor) => {
                  setGerenciaId(valor);
                  setGerenciaArchivo(null);
                  setGerenciaQuitar(true);
                  setGerenciaClave((actual) => actual + 1);
                  if (!valor) setGerenciaVirtual(false);
                }}
                onFecha={setGerenciaFecha}
                onVirtual={(valor) => {
                  setGerenciaVirtual(valor);
                  if (!valor) {
                    setGerenciaArchivo(null);
                    setGerenciaClave((actual) => actual + 1);
                  }
                }}
                onArchivo={(archivo) => elegirFirma(
                  archivo,
                  setGerenciaArchivo,
                  () => setGerenciaQuitar(false),
                  () => setGerenciaClave((actual) => actual + 1),
                )}
                onQuitar={() => {
                  setGerenciaArchivo(null);
                  setGerenciaQuitar(true);
                  setGerenciaClave((actual) => actual + 1);
                }}
              />
            </div>
          </section>
        </>
      )}

      {error && <p className="mt-3 text-sm text-copper-dark" role="alert">{error}</p>}
      {aviso && <p className="mt-3 text-sm text-pine">{aviso}</p>}
      {listo && (
        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" disabled={guardando || !datos} className="rounded-md bg-pine px-3 py-2 text-sm font-semibold text-white disabled:opacity-60" onClick={() => void guardar()}>
            Guardar informe
          </button>
          <button type="button" disabled={!datos} className="rounded-md border border-line bg-card px-3 py-2 text-sm font-semibold disabled:opacity-60" onClick={() => void bajar()}>
            Descargar informe
          </button>
        </div>
      )}
      {listo && <p className="mt-2 text-xs text-muted">Guarde el informe para que el PDF incluya el análisis y las firmas.</p>}
    </section>
  );
}

function Lista({ titulo, items }: { titulo: string; items: Corte[] }) {
  return (
    <div className="mt-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-muted">{titulo}</p>
      {items.length === 0 && <p className="mt-1 text-sm text-muted">Sin reclamos.</p>}
      <ul className="mt-1 space-y-1 text-sm">
        {items.map((item) => (
          <li key={item.nombre} className="flex justify-between gap-3">
            <span>{item.nombre}</span>
            <span className="text-muted">{item.cantidad} · {item.porcentaje}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Estado({ valor }: { valor: string }) {
  return <p className="mt-2 text-sm font-semibold">{valor}</p>;
}

function Firma({
  rol,
  personas,
  personaId,
  fecha,
  virtual,
  vista,
  clave,
  onPersona,
  onFecha,
  onVirtual,
  onArchivo,
  onQuitar,
}: {
  rol: string;
  personas: Firmante[];
  personaId: string;
  fecha: string;
  virtual: boolean;
  vista: string;
  clave: number;
  onPersona: (valor: string) => void;
  onFecha: (valor: string) => void;
  onVirtual: (valor: boolean) => void;
  onArchivo: (archivo: File | null) => void;
  onQuitar: () => void;
}) {
  const elegida = personas.find((persona) => String(persona.id) === personaId);
  return (
    <div className="rounded-md border border-line p-3">
      <p className="text-sm font-semibold">{rol}</p>
      <label className="mt-2 block text-sm font-semibold">
        Persona asignada
        <select className={campo} value={personaId} onChange={(evento) => onPersona(evento.target.value)}>
          <option value="">Seleccione</option>
          {personas.map((persona) => (
            <option key={persona.id} value={persona.id}>
              {persona.nombre} · {persona.cargo}
            </option>
          ))}
        </select>
      </label>
      <p className="mt-2 text-sm text-muted">Perfil: {elegida ? `${elegida.cargo} · ${elegida.departamento}` : "Sin persona"}</p>
      <div className="mt-2">
        <CampoFecha id={`firma-${rol}`} label="Fecha" value={fecha} onChange={onFecha} onBlur={() => undefined} />
      </div>
      <label className="mt-3 flex items-center gap-2 text-sm font-semibold">
        <input
          type="checkbox"
          checked={virtual}
          disabled={!personaId}
          onChange={(evento) => onVirtual(evento.target.checked)}
        />
        Firma virtual
      </label>
      {virtual && personaId && (
        <div className="mt-3">
          <label className="block text-sm font-semibold">
            Subir firma
            <input
              key={clave}
              type="file"
              accept="image/png,image/jpeg,.png,.jpg,.jpeg"
              className="mt-1 block w-full text-sm"
              onChange={(evento) => onArchivo(evento.target.files?.[0] ?? null)}
            />
          </label>
          <p className="mt-1 text-xs text-muted">PNG o JPG, hasta 1 MB. Al guardar queda en el informe.</p>
          {vista && (
            <img src={vista} alt={`Firma de ${rol}`} className="mt-2 h-16 max-w-full rounded-md border border-line bg-white object-contain p-1" />
          )}
          {vista && (
            <button type="button" className="mt-2 text-sm font-semibold text-copper-dark" onClick={onQuitar}>
              Quitar firma
            </button>
          )}
        </div>
      )}
      <p className="mt-2 text-sm text-muted">Firma: {virtual && personaId ? "Firma virtual" : "Pendiente"}</p>
    </div>
  );
}
