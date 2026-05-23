import { useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import "./App.css";
import DashboardPage from "./DashboardPage";
import CompromissosPage from "./CompromissosPage";
import PrestadoresPage from "./PrestadoresPage";
import AnotacoesPage from "./AnotacoesPage";
import MoradoresPage from "./MoradoresPage";
import ManutencoesPage from "./ManutencoesPage";
import ReunioesPage from "./ReunioesPage";
import GastosPage from "./GastosPage";
import AssistenteIAPage from "./AssistenteIAPage";
import LoginPage from "./LoginPage";
import AdminPage from "./AdminPage";
import { AUTH_EXPIRED_EVENT, getMe, logout } from "./api";

const PAGES = {
  dashboard: "Início",
  compromissos: "Compromissos",
  manutencoes: "Manutenções",
  reunioes: "Reuniões",
  anotacoes: "Anotações",
  moradores: "Moradores",
  prestadores: "Prestadores",
  gastos: "Controle de Gasto",
};

const IA_PAGES = {
  assistente: "Assistente IA",
};

function App() {
  // null = ainda verificando; false = não autenticado; objeto = usuário logado
  const [user, setUser] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [toolbarGlow, setToolbarGlow] = useState({ x: "50%", y: "50%", visible: false });
  const [now, setNow] = useState(() => new Date());
  const allPages = Object.entries(PAGES);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const sindicoNome = user?.nome || user?.email || "Síndico";
  const condominioNome =
    user?.nomeCondominio || user?.condominioNome || user?.condominio?.nome || "Condomínio";
  const dataHoraTexto = now.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "medium",
  });

  useEffect(() => {
    let active = true;

    getMe()
      .then((data) => {
        if (!active) return;
        setUser(data);
      })
      .finally(() => {
        if (!active) return;
        setAuthChecked(true);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    function handleAuthExpired() {
      setMenuOpen(false);
      setUser(false);
    }

    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, []);

  useEffect(() => {
    const timerId = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timerId);
  }, []);

  async function handleLogout() {
    await logout();
    setUser(false);
  }

  function closeMenu() {
    setMenuOpen(false);
  }

  function handleToolbarMouseMove(event) {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = `${event.clientX - rect.left}px`;
    const y = `${event.clientY - rect.top}px`;
    setToolbarGlow({ x, y, visible: true });
  }

  function handleToolbarMouseLeave() {
    setToolbarGlow((prev) => ({ ...prev, visible: false }));
  }

  if (!authChecked) {
    return (
      <main className="page">
        <nav className="nav">
          <span className="nav-brand">LiveSindIA</span>
        </nav>
        <section className="hero">
          <p className="muted">Verificando sessão...</p>
        </section>
      </main>
    );
  }

  if (!user) {
    return <LoginPage onLogin={(u) => setUser(u)} />;
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar-brand">LiveSindIA</div>
        <nav className="sidebar-nav" aria-label="Módulos">
          {allPages.map(([key, label]) => (
            <NavLink
              key={key}
              to={`/${key}`}
              onClick={closeMenu}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? "sidebar-link--active" : ""}`
              }
            >
              {label}
            </NavLink>
          ))}

          <div className="sidebar-divider" />
          <span className="sidebar-section-label">IA</span>
          {Object.entries(IA_PAGES).map(([key, label]) => (
            <NavLink
              key={key}
              to={`/${key}`}
              onClick={closeMenu}
              className={({ isActive }) =>
                `sidebar-link sidebar-link--ia ${isActive ? "sidebar-link--active" : ""}`
              }
            >
              {label}
            </NavLink>
          ))}

          {isAdmin && (
            <NavLink
              to="/admin"
              onClick={closeMenu}
              className={({ isActive }) =>
                `sidebar-link sidebar-link--admin ${isActive ? "sidebar-link--active" : ""}`
              }
            >
              Admin
            </NavLink>
          )}
        </nav>
        <button className="sidebar-logout" onClick={handleLogout} title={user.email}>
          Sair
        </button>
      </aside>

      {menuOpen ? <button className="sidebar-backdrop" onClick={closeMenu} aria-label="Fechar menu" /> : null}

      <main className="content-wrap">
        <header
          className="content-header"
          onMouseMove={handleToolbarMouseMove}
          onMouseLeave={handleToolbarMouseLeave}
          style={{
            "--toolbar-glow-x": toolbarGlow.x,
            "--toolbar-glow-y": toolbarGlow.y,
            "--toolbar-glow-opacity": toolbarGlow.visible ? 1 : 0,
          }}
        >
          <button className="menu-toggle" onClick={() => setMenuOpen((v) => !v)} aria-label="Abrir menu">
            Menu
          </button>
          <div className="content-user" title={`${condominioNome} · ${sindicoNome}`}>
            <span className="content-user-line content-user-line--primary">{condominioNome}</span>
            <span className="content-user-line">Síndico: {sindicoNome}</span>
            <span className="content-user-line">Data e hora: {dataHoraTexto}</span>
          </div>
        </header>

        <section className="page">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/compromissos" element={<CompromissosPage />} />
            <Route path="/manutencoes" element={<ManutencoesPage />} />
            <Route path="/reunioes" element={<ReunioesPage />} />
            <Route path="/anotacoes" element={<AnotacoesPage />} />
            <Route path="/moradores" element={<MoradoresPage />} />
            <Route path="/prestadores" element={<PrestadoresPage />} />
            <Route path="/gastos" element={<GastosPage />} />
            <Route path="/assistente" element={<AssistenteIAPage />} />
            <Route
              path="/config-ia"
              element={<Navigate to={isAdmin ? "/admin" : "/assistente"} replace />}
            />
            <Route
              path="/admin"
              element={isAdmin ? <AdminPage /> : <Navigate to="/compromissos" replace />}
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </section>
      </main>
    </div>
  );
}

export default App;
