import { useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes, useLocation } from "react-router-dom";
import "./App.css";
import DashboardPage from "./DashboardPage";
import CondominioPage from "./CondominioPage";
import PerfilPage from "./PerfilPage";
import EsqueciSenhaPage from "./EsqueciSenhaPage";
import RedefinirSenhaPage from "./RedefinirSenhaPage";
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
import { AUTH_EXPIRED_EVENT, getCondominioSelecionado, getMe, listarCondominios, logout } from "./api";

const NAVIGATION_GROUPS = [
  {
    label: "Operação",
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
    pages: [
      ["moradores", "Moradores"],
      ["prestadores", "Prestadores"],
    ],
  },
  {
    label: "Financeiro",
    pages: [["gastos", "Financeiro"]],
  },
  {
    label: "Conta e condomínio",
    pages: [
      ["condominio", "Condomínio"],
      ["perfil", "Meu perfil"],
    ],
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
  const location = useLocation();
  const [condominioAtivoNome, setCondominioAtivoNome] = useState("");
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const sindicoNome = user?.nome || user?.email || "Síndico";
  const condominioNome =
    condominioAtivoNome ||
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

  // Mostra o nome do condominio escolhido pelo usuario (o padrao vem do /me).
  useEffect(() => {
    const escolhidoId = getCondominioSelecionado();
    if (!user || !escolhidoId) return undefined;
    let ativo = true;
    listarCondominios()
      .then((lista) => {
        const escolhido = lista.find((c) => c.id === escolhidoId);
        if (ativo && escolhido) setCondominioAtivoNome(escolhido.nome);
      })
      .catch(() => {});
    return () => {
      ativo = false;
    };
  }, [user]);

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

  // Rotas publicas de recuperacao de senha: nao exigem sessao (o link chega por e-mail).
  if (location.pathname === "/esqueci-senha") return <EsqueciSenhaPage />;
  if (location.pathname === "/redefinir-senha") return <RedefinirSenhaPage />;
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
      <aside id="app-sidebar" className={`sidebar ${menuOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar-brand">LiveSindIA</div>
        <nav className="sidebar-nav" aria-label="Módulos">
          {NAVIGATION_GROUPS.map(({ label, pages }) => (
            <div className="sidebar-nav-group" key={label}>
              <span className="sidebar-section-label">{label}</span>
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
            <Route path="/condominio" element={<CondominioPage />} />
            <Route path="/perfil" element={<PerfilPage />} />
            <Route
              path="/condominios/selecionar"
              element={<Navigate to="/condominio" replace />}
            />
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
