import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api } from "../api/client";

export type Usuario = {
  email: string;
  nombre: string;
  departamento: string;
};

type Sesion = {
  usuario: Usuario | null;
  listo: boolean;
  ingresar: (correo: string, contrasena: string) => Promise<void>;
  salir: () => Promise<void>;
};

const Contexto = createContext<Sesion | null>(null);

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [listo, setListo] = useState(false);

  useEffect(() => {
    api<Usuario>("/api/auth/me")
      .then(setUsuario)
      .catch(() => setUsuario(null))
      .finally(() => setListo(true));
  }, []);

  const valor = useMemo<Sesion>(
    () => ({
      usuario,
      listo,
      async ingresar(correo, contrasena) {
        const sesion = await api<Usuario>("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({ correo, contrasena }),
        });
        setUsuario(sesion);
      },
      async salir() {
        await api<void>("/api/auth/logout", { method: "POST" });
        setUsuario(null);
      },
    }),
    [usuario, listo],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useSesion() {
  const sesion = useContext(Contexto);
  if (!sesion) {
    throw new Error("La sesión no está disponible.");
  }
  return sesion;
}
