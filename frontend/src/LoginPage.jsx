import { useState } from 'react'
import { login, register } from './api'

function LoginPage({ onLogin }) {
  const [mode, setMode] = useState('login')
  const [nome, setNome] = useState('')
  const [nomeCondominio, setNomeCondominio] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmarSenha, setConfirmarSenha] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      if (mode === 'register') {
        await register({ nome, email, nomeCondominio, senha, confirmarSenha })
        setSuccess('Cadastro realizado. Entrando na sua conta...')
      }
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
        <span className="nav-brand">LiveSindIA</span>
      </nav>

      <section className="hero">
        <p className="eyebrow">Gestão de condomínio</p>
        <h1>{mode === 'login' ? 'Entrar' : 'Criar conta'}</h1>
        <p className="subtitle">
          {mode === 'login'
            ? 'Acesse sua conta para gerenciar o condomínio.'
            : 'Cadastre seu condomínio e tenha um login exclusivo de síndico.'}
        </p>
      </section>

      <section className="layout" style={{ justifyContent: 'center' }}>
        <article className="panel" style={{ maxWidth: '420px', width: '100%' }}>
          <h2>{mode === 'login' ? 'Login' : 'Cadastro'}</h2>
          <div className="auth-mode-toggle" aria-label="Alternar entre login e cadastro">
            <button
              type="button"
              className={`mode-button ${mode === 'login' ? 'mode-button--active' : ''}`}
              onClick={() => setMode('login')}
              disabled={loading}
            >
              Entrar
            </button>
            <button
              type="button"
              className={`mode-button ${mode === 'register' ? 'mode-button--active' : ''}`}
              onClick={() => setMode('register')}
              disabled={loading}
            >
              Cadastrar
            </button>
          </div>
          <form onSubmit={onSubmit} className="form-grid auth-form">
            {mode === 'register' ? (
              <>
                <label>
                  Nome do síndico
                  <input value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={150} />
                </label>
                <label>
                  Nome do condomínio
                  <input value={nomeCondominio} onChange={(e) => setNomeCondominio(e.target.value)} required maxLength={150} />
                </label>
              </>
            ) : null}
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
              <span className="field-head">
                <span>Senha</span>
                {mode === 'login' ? <a href="/esqueci-senha">Esqueci minha senha</a> : null}
              </span>
              <input
                type="password"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                required
                autoComplete="current-password"
              />
            </label>
            {mode === 'register' ? (
              <label>
                Confirmar senha
                <input
                  type="password"
                  value={confirmarSenha}
                  onChange={(e) => setConfirmarSenha(e.target.value)}
                  required
                  autoComplete="new-password"
                />
              </label>
            ) : null}
            {error ? <p className="message error">{error}</p> : null}
            {success ? <p className="message success">{success}</p> : null}
            <button type="submit" disabled={loading} className="submit full">
              {loading ? 'Processando...' : mode === 'login' ? 'Entrar' : 'Cadastrar e entrar'}
            </button>
          </form>
          <p className="auth-footer">
            {mode === 'login' ? 'Não tem conta? ' : 'Já tem conta? '}
            <button
              type="button"
              className="link-button"
              onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
              disabled={loading}
            >
              {mode === 'login' ? 'Criar conta gratuita' : 'Entrar'}
            </button>
          </p>
        </article>
      </section>
    </main>
  )
}

export default LoginPage

