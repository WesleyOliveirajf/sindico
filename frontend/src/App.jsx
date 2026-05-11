import { useEffect, useState } from 'react'
import './App.css'
import CompromissosPage from './CompromissosPage'
import PrestadoresPage from './PrestadoresPage'
import AnotacoesPage from './AnotacoesPage'
import MoradoresPage from './MoradoresPage'
import ManutencoesPage from './ManutencoesPage'
import ReunioesPage from './ReunioesPage'
import LoginPage from './LoginPage'
import { getMe, logout } from './api'

const PAGES = {
  compromissos: 'Compromissos',
  manutencoes: 'Manutencoes',
  reunioes: 'Reunioes',
  anotacoes: 'Anotacoes',
  moradores: 'Moradores',
  prestadores: 'Prestadores',
}

function App() {
  const [page, setPage] = useState(() => {
    return sessionStorage.getItem('appPage') || 'compromissos'
  })
  // null = ainda verificando; false = nao autenticado; objeto = usuario logado
  const [user, setUser] = useState(null)
  const [authChecked, setAuthChecked] = useState(false)

  useEffect(() => {
    sessionStorage.setItem('appPage', page)
  }, [page])

  useEffect(() => {
    let active = true

    getMe()
      .then((data) => {
        if (!active) return
        setUser(data)
      })
      .finally(() => {
        if (!active) return
        setAuthChecked(true)
      })

    return () => {
      active = false
    }
  }, [])

  async function handleLogout() {
    await logout()
    setUser(false)
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
    )
  }

  if (!user) {
    return <LoginPage onLogin={(u) => setUser(u)} />
  }

  return (
    <main className="page">
      <nav className="nav">
        <span className="nav-brand">Sindico App</span>
        <div className="nav-links">
          {Object.entries(PAGES).map(([key, label]) => (
            <button
              key={key}
              className={`nav-link ${page === key ? 'nav-link--active' : ''}`}
              onClick={() => setPage(key)}
            >
              {label}
            </button>
          ))}
        </div>
        <button className="nav-link" onClick={handleLogout} title={user.email}>
          Sair
        </button>
      </nav>

      {page === 'compromissos' && <CompromissosPage />}
      {page === 'manutencoes' && <ManutencoesPage />}
      {page === 'reunioes' && <ReunioesPage />}
      {page === 'anotacoes' && <AnotacoesPage />}
      {page === 'moradores' && <MoradoresPage />}
      {page === 'prestadores' && <PrestadoresPage />}
    </main>
  )
}

export default App
