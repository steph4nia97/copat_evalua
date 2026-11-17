import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api, ErrorApi } from "../api/client";
import { Encabezado } from "../components/Encabezado";

type Cruce = {
  rut: string;
  empresa: string;
  nombreContacto: string | null;
  empresaContacto: string | null;
  correo: string | null;
  telefono: string | null;
};

type Carga = {
  guardados: number;
  omitidos: number;
  avisos: string[];
};

export function CargaDatos() {
  const queryClient = useQueryClient();
  const [avisoClientes, setAvisoClientes] = useState("");
  const [avisoContactos, setAvisoContactos] = useState("");
  const [errorClientes, setErrorClientes] = useState("");
  const [errorContactos, setErrorContactos] = useState("");
  const cruce = useQuery({
    queryKey: ["cruce-clientes"],
    queryFn: () => api<Cruce[]>("/api/departamento1/cruce"),
  });
  const filas = cruce.data ?? [];

  async function subir(ruta: string, archivo: File | null, destino: "clientes" | "contactos") {
    const setAviso = destino === "clientes" ? setAvisoClientes : setAvisoContactos;
    const setError = destino === "clientes" ? setErrorClientes : setErrorContactos;
    setAviso("");
    setError("");
    if (!archivo) {
      return;
    }
    const datos = new FormData();
    datos.set("archivo", archivo);
    try {
      const carga = await api<Carga>(ruta, { method: "POST", body: datos });
      const detalle = carga.avisos.length ? ` ${carga.avisos[0]}` : "";
      setAviso(`Se guardaron ${carga.guardados}. Se omitieron ${carga.omitidos}.${detalle}`);
      await queryClient.invalidateQueries({ queryKey: ["cruce-clientes"] });
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No se pudo leer el Excel.");
    }
  }

  return (
    <div className="mt-5 grid gap-6">
      <section className="rounded-xl border border-line bg-card p-5">
        <h2 className="font-serif text-2xl">Datos de clientes</h2>
        <p className="mt-1 text-sm text-muted">
          El archivo puede traer más columnas. Se usan RUT y Razón social o Nombre empresa. Guía de despacho puede quedar en el Excel: si el mismo RUT se repite, se guarda un solo cliente.
        </p>
        <input
          type="file"
          accept=".xlsx,.xls"
          aria-label="Excel de datos de clientes"
          className="mt-3 block w-full text-sm"
          onChange={(evento) => void subir("/api/departamento1/clientes", evento.target.files?.[0] ?? null, "clientes")}
        />
        {avisoClientes && <p className="mt-3 text-sm text-pine">{avisoClientes}</p>}
        {errorClientes && (
          <p className="mt-3 text-sm text-copper-dark" role="alert">
            {errorClientes}
          </p>
        )}
      </section>

      <section className="rounded-xl border border-line bg-card p-5">
        <h2 className="font-serif text-2xl">Contacto de clientes</h2>
        <p className="mt-1 text-sm text-muted">
          Columnas: RUT, Razón social, Nombre contacto, Correo y Teléfono. El contacto se cruza con el cliente por el RUT, así que ese RUT tiene que estar cargado antes. Si el correo o el teléfono vienen vacíos, se guardan como No aplica.
        </p>
        <input
          type="file"
          accept=".xlsx,.xls"
          aria-label="Excel de contacto de clientes"
          className="mt-3 block w-full text-sm"
          onChange={(evento) => void subir("/api/departamento1/contactos", evento.target.files?.[0] ?? null, "contactos")}
        />
        {avisoContactos && <p className="mt-3 text-sm text-pine">{avisoContactos}</p>}
        {errorContactos && (
          <p className="mt-3 text-sm text-copper-dark" role="alert">
            {errorContactos}
          </p>
        )}
      </section>

      <section className="rounded-xl border border-line bg-card p-5">
        <h2 className="font-serif text-2xl">Cruce</h2>
        <p className="mt-1 text-sm text-muted">
          Cada cliente queda junto al contacto del mismo RUT. Si el cliente no tiene contacto, la fila lo indica.
        </p>
        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-paper text-muted">
              <tr>
                {["Cliente", "RUT", "Contacto", "Correo", "Teléfono"].map((columna) => (
                  <th key={columna} className="px-3 py-2 font-semibold">
                    {columna}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filas.map((fila) => (
                <tr key={`${fila.rut}-${fila.correo ?? "sin"}-${fila.nombreContacto ?? "sin"}`} className="border-t border-line">
                  <td className="px-3 py-3 font-semibold">{fila.empresa}</td>
                  <td className="px-3 py-3">{fila.rut}</td>
                  <td className="px-3 py-3">{fila.nombreContacto ?? "Sin contacto"}</td>
                  <td className="px-3 py-3">{fila.correo ?? "—"}</td>
                  <td className="px-3 py-3">{fila.telefono ?? "—"}</td>
                </tr>
              ))}
              {filas.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-3 py-8 text-center text-muted">
                    Todavía no hay datos para cruzar.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export function ClientesPage() {
  return (
    <div className="min-h-screen">
      <Encabezado />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <h1 className="font-serif text-3xl">Carga de datos</h1>
        <CargaDatos />
      </main>
    </div>
  );
}
