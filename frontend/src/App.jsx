import { useEffect, useState } from 'react'
import './App.css'
import CompromissosPage from './CompromissosPage'
import PrestadoresPage from './PrestadoresPage'
import AnotacoesPage from './AnotacoesPage'
import MoradoresPage from './MoradoresPage'

const PAGES = {
  compromissos: 'Compromissos',
  anotacoes: 'Anotacoes',
  moradores: 'Moradores',
  prestadores: 'Prestadores',
}

function App() {
  const [page, setPage] = useState(() => {
    return sessionStorage.getItem('appPage') || 'compromissos'
  })

  useEffect(() => {
    sessionStorage.setItem('appPage', page)
  }, [page])

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
      </nav>

      {page === 'compromissos' && <CompromissosPage />}
      {page === 'anotacoes' && <AnotacoesPage />}
      {page === 'moradores' && <MoradoresPage />}
      {page === 'prestadores' && <PrestadoresPage />}
    </main>
  )
}

export default App
