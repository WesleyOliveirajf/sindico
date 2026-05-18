import { useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import "./App.css";
import CompromissosPage from "./CompromissosPage";
import PrestadoresPage from "./PrestadoresPage";
import AnotacoesPage from "./AnotacoesPage";
import MoradoresPage from "./MoradoresPage";
import ManutencoesPage from "./ManutencoesPage";
import ReunioesPage from "./ReunioesPage";
import GastosPage from "./GastosPage";
import LoginPage from "./LoginPage";
import { AUTH_EXPIRED_EVENT, getMe, logout } from "./api";

const PAGES = {
  compromissos: "Compromissos",
  manutencoes: "Manutencoes",
  reunioes: "Reunioes",
  anotacoes: "Anotacoes",
  moradores: "Moradores",
  prestadores: "Prestadores",
  gastos: "Gastos",
};

function App() {
  // null = ainda verificando; false = nao autenticado; objeto = usuario logado
  const [user, setUser] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const allPages = Object.entries(PAGES);

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
          <span className="nav-brand">Sindico App</span>
        </nav>
        <section className="hero">
          <p className="muted">Verificando sessao...</p>
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
        <div className="sidebar-brand">Sindico App</div>
        <nav className="sidebar-nav" aria-label="Modulos">
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
        </nav>
        <button className="sidebar-logout" onClick={handleLogout} title={user.email}>
          Sair
        </button>
      </aside>

      {menuOpen ? <button className="sidebar-backdrop" onClick={closeMenu} aria-label="Fechar menu" /> : null}

      <main className="content-wrap">
        <header className="content-header">
          <button className="menu-toggle" onClick={() => setMenuOpen((v) => !v)} aria-label="Abrir menu">
            Menu
          </button>
          <span className="content-user" title={user.email}>{user.nome || user.email}</span>
        </header>

        <section className="page">
          <Routes>
            <Route path="/" element={<Navigate to="/compromissos" replace />} />
            <Route path="/compromissos" element={<CompromissosPage />} />
            <Route path="/manutencoes" element={<ManutencoesPage />} />
            <Route path="/reunioes" element={<ReunioesPage />} />
            <Route path="/anotacoes" element={<AnotacoesPage />} />
            <Route path="/moradores" element={<MoradoresPage />} />
            <Route path="/prestadores" element={<PrestadoresPage />} />
            <Route path="/gastos" element={<GastosPage />} />
            <Route path="*" element={<Navigate to="/compromissos" replace />} />
          </Routes>
        </section>
      </main>
    </div>
  );
}

export default App;
