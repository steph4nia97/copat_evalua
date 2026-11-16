import { useSesion } from "../auth/sesion";
import { Encabezado } from "../components/Encabezado";
import { Bitacora } from "../components/Bitacora";

export function BitacoraPage() {
  const { usuario } = useSesion();
  const ruta = usuario?.departamentoId === 3
    ? "/api/departamento3/bitacora"
    : "/api/departamento1/bitacora";

  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <h1 className="font-serif text-3xl">Bitácora</h1>
        <p className="mt-1 text-sm text-muted">
          Carga de datos, envío y recepción de la solicitud, cómo fue evaluada y el estado del seguimiento.
        </p>
        <Bitacora ruta={ruta} encabezado={false} />
      </main>
    </div>
  );
}
