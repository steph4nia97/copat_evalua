import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage";
import { SolicitudesPage } from "./pages/SolicitudesPage";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<SolicitudesPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
