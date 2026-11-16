import { type ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useSesion } from "./auth/sesion";
import { AjustesPage } from "./pages/AjustesPage";
import { BitacoraPage } from "./pages/BitacoraPage";
import { AdminPage } from "./pages/AdminPage";
import { ClientesPage } from "./pages/ClientesPage";
import { Departamento2Page } from "./pages/Departamento2Page";
import { Departamento3Page } from "./pages/Departamento3Page";
import { EncuestaPublicaPage } from "./pages/EncuestaPublicaPage";
import { IndicadoresPage } from "./pages/IndicadoresPage";
import { LoginPage } from "./pages/LoginPage";
import { ReclamoPublicoPage } from "./pages/ReclamoPublicoPage";
import { ReclamosPage } from "./pages/ReclamosPage";
import { SolicitudesPage } from "./pages/SolicitudesPage";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/encuesta" element={<EncuestaPublicaPage />} />
      <Route path="/encuesta/:token" element={<EncuestaPublicaPage />} />
      <Route path="/reclamo" element={<ReclamoPublicoPage />} />
      <Route path="/reclamos" element={<Protegida departamento={3}><ReclamosPage /></Protegida>} />
      <Route path="/clientes" element={<Protegida departamento={[1, 2]}><ClientesPage /></Protegida>} />
      <Route path="/" element={<Inicio />} />
      <Route path="/ajustes" element={<Protegida><AjustesPage /></Protegida>} />
      <Route path="/indicadores" element={<Protegida departamento={3}><IndicadoresPage /></Protegida>} />
      <Route path="/bitacora" element={<Protegida departamento={[1, 2, 3]}><BitacoraPage /></Protegida>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function Protegida({ children, departamento }: { children: ReactNode; departamento?: number | number[] }) {
  const { usuario, listo } = useSesion();
  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }
  const permitidos = departamento == null ? null : Array.isArray(departamento) ? departamento : [departamento];
  if (permitidos && !permitidos.includes(usuario.departamentoId)) {
    return <Navigate to="/" replace />;
  }
  return children;
}

function Inicio() {
  const { usuario, listo } = useSesion();
  if (!listo) {
    return <p className="p-8 text-muted">Cargando…</p>;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }
  if (usuario.departamentoId === 1 || usuario.departamentoId === 2) {
    return <Departamento2Page />;
  }
  if (usuario.departamentoId === 3) {
    return <Departamento3Page />;
  }
  if (usuario.departamentoId === 4) {
    return <AdminPage />;
  }
  return <SolicitudesPage />;
}
