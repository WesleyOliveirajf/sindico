import { useState } from 'react'
import { login } from './api'

function LoginPage({ onLogin }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = await login(email, senha)
      onLogin(user)
    } catch (err) {
      setError(err.message || 'Credenciais invalidas')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <nav className="nav">
        <span className="nav-brand">Sindico App</span>
      </nav>

      <section className="hero">
        <p className="eyebrow">Gestao de condominio</p>
        <h1>Entrar</h1>
        <p className="subtitle">Acesse sua conta para gerenciar o condominio.</p>
      </section>

      <section className="layout" style={{ justifyContent: 'center' }}>
        <article className="panel" style={{ maxWidth: '420px', width: '100%' }}>
          <h2>Login</h2>
          <form onSubmit={onSubmit} className="form-grid">
            <label>
              E-mail
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="sindico@demo.local"
              />
            </label>
            <label>
              Senha
              <input
                type="password"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                required
                autoComplete="current-password"
              />
            </label>
            {error ? <p className="message error">{error}</p> : null}
            <button type="submit" disabled={loading} className="submit full">
              {loading ? 'Entrando...' : 'Entrar'}
            </button>
          </form>
        </article>
      </section>
    </main>
  )
}

export default LoginPage
