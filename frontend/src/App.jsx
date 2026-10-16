import { lazy, Suspense, useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import "./App.css";
import { LoadingState } from "./shared/components/PageFeedback";

// Code splitting por rota: o bundle inicial carrega só o shell; cada módulo vem sob demanda.
const DashboardPage = lazy(() => import("./features/dashboard/DashboardPage"));
const CompromissosPage = lazy(() => import("./features/compromissos/CompromissosPage"));
const PrestadoresPage = lazy(() => import("./features/prestadores/PrestadoresPage"));
const AnotacoesPage = lazy(() => import("./features/anotacoes/AnotacoesPage"));
const MoradoresPage = lazy(() => import("./features/moradores/MoradoresPage"));
const ManutencoesPage = lazy(() => import("./features/manutencoes/ManutencoesPage"));
const ReunioesPage = lazy(() => import("./features/reunioes/ReunioesPage"));
const GastosPage = lazy(() => import("./features/gastos/GastosPage"));
const AssistenteIAPage = lazy(() => import("./features/ia/AssistenteIAPage"));
const LoginPage = lazy(() => import("./features/auth/LoginPage"));
const AdminPage = lazy(() => import("./features/admin/AdminPage"));
import { AUTH_EXPIRED_EVENT, getMe, logout } from "./shared/api";

const NAVIGATION_GROUPS = [
  {
    label: "Operações",
    hint: "Rotina do condomínio",
    pages: [
      ["dashboard", "Início"],
      ["compromissos", "Lembretes"],
      ["manutencoes", "Manutenções"],
      ["reunioes", "Reuniões"],
      ["anotacoes", "Anotações"],
    ],
  },
  {
    label: "Pessoas",
    hint: "Moradores e prestadores",
    pages: [
      ["moradores", "Moradores"],
      ["prestadores", "Prestadores"],
    ],
  },
  {
    label: "Finanças",
    hint: "Gastos e recebimentos",
    pages: [["gastos", "Gastos e recebimentos"]],
  },
];

const IA_PAGES = {
  assistente: "Assistente IA",
};

function App() {
  // null = ainda verificando; false = não autenticado; objeto = usuário logado
  const [user, setUser] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const sindicoNome = user?.nome || user?.email || "Síndico";
  const condominioNome =
    user?.nomeCondominio || user?.condominioNome || user?.condominio?.nome || "Condomínio";
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
    document.body.classList.toggle("menu-open", menuOpen);
    return () => document.body.classList.remove("menu-open");
  }, [menuOpen]);

  async function handleLogout() {
    await logout();
    setUser(false);
  }

  function closeMenu() {
    setMenuOpen(false);
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
    return (
      <Suspense fallback={<LoadingState message="Carregando..." />}>
        <LoginPage onLogin={(u) => setUser(u)} />
      </Suspense>
    );
  }

  return (
    <div className="app-shell">
      <aside id="app-sidebar" className={`sidebar ${menuOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar-brand">LiveSindIA</div>
        <nav className="sidebar-nav" aria-label="Módulos">
          {NAVIGATION_GROUPS.map(({ label, hint, pages }) => (
            <div className="sidebar-nav-group" key={label}>
              <div className="sidebar-section-heading">
                <span className="sidebar-section-label">{label}</span>
                <span className="sidebar-section-hint">{hint}</span>
              </div>
              {pages.map(([key, pageLabel]) => (
                <NavLink
                  key={key}
                  to={`/${key}`}
                  onClick={closeMenu}
                  className={({ isActive }) =>
                    `sidebar-link ${isActive ? "sidebar-link--active" : ""}`
                  }
                >
                  {pageLabel}
                </NavLink>
              ))}
            </div>
          ))}

          <div className="sidebar-nav-group sidebar-nav-group--ia">
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
          </div>

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
        <header className="content-header">
          <button
            className="menu-toggle"
            onClick={() => setMenuOpen((v) => !v)}
            aria-label={menuOpen ? "Fechar menu" : "Abrir menu"}
            aria-expanded={menuOpen}
            aria-controls="app-sidebar"
          >
            {menuOpen ? "Fechar" : "Menu"}
          </button>
          <div className="content-user" title={`${condominioNome} · ${sindicoNome}`}>
            <span className="content-user-line content-user-line--primary">{condominioNome}</span>
            <span className="content-user-line content-user-line--secondary">Olá, {sindicoNome}</span>
          </div>
        </header>

        <section className="page">
          <Suspense fallback={<LoadingState />}>
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
          </Suspense>
        </section>

        <footer className="app-global-footer">
          Criado e desenvolvido por{' '}
          <a href="https://www.instagram.com/analisandoIA" target="_blank" rel="noopener noreferrer">
            @analisandoIA
          </a>
        </footer>
      </main>
    </div>
  );
}

export default App;
