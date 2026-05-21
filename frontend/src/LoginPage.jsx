import { useState } from 'react'
import { login, register } from './api'

function LoginPage({ onLogin }) {
  const [mode, setMode] = useState('login')
  const [nome, setNome] = useState('')
  const [nomeCondominio, setNomeCondominio] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmarSenha, setConfirmarSenha] = useState('')
  const [aceitouTermos, setAceitouTermos] = useState(false)
  const [aceitouMarketing, setAceitouMarketing] = useState(false)
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
        if (!aceitouTermos) {
          throw new Error('Você precisa ler e aceitar os Termos de Uso e a Política de Privacidade.')
        }
        await register({ nome, email, nomeCondominio, senha, confirmarSenha, aceitouTermos, aceitouMarketing })
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
            {mode === 'register' ? (
              <div className="lgpd-checkboxes-container" style={{ margin: '15px 0', fontSize: '0.85rem', display: 'flex', flexDirection: 'column', gap: '12px', width: '100%', gridColumn: '1 / -1' }}>
                <label style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', cursor: 'pointer', fontWeight: 'normal', color: 'var(--ink)' }}>
                  <input
                    type="checkbox"
                    checked={aceitouTermos}
                    onChange={(e) => setAceitouTermos(e.target.checked)}
                    required
                    style={{ width: 'auto', marginTop: '3px', flexShrink: 0 }}
                  />
                  <span style={{ lineHeight: '1.4', textAlign: 'left' }}>
                    Declaro que li e aceito os <a href="/termos" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent)', fontWeight: 600, textDecoration: 'none' }}>Termos de Uso</a> e a <a href="/privacidade" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent)', fontWeight: 600, textDecoration: 'none' }}>Política de Privacidade</a> da plataforma, e estou ciente de que meus dados pessoais serão tratados para criação da conta, autenticação, prestação dos serviços contratados, suporte, segurança da plataforma e cumprimento de obrigações legais.
                  </span>
                </label>

                <label style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', cursor: 'pointer', fontWeight: 'normal', color: 'var(--ink)' }}>
                  <input
                    type="checkbox"
                    checked={aceitouMarketing}
                    onChange={(e) => setAceitouMarketing(e.target.checked)}
                    style={{ width: 'auto', marginTop: '3px', flexShrink: 0 }}
                  />
                  <span style={{ lineHeight: '1.4', textAlign: 'left' }}>
                    Aceito receber comunicações comerciais, novidades e ofertas da plataforma por e-mail, WhatsApp ou telefone. Posso cancelar esse recebimento a qualquer momento.
                  </span>
                </label>
                
                <div style={{ textAlign: 'center', marginTop: '4px', fontSize: '0.78rem', color: 'var(--muted)' }}>
                  Consulte também a nossa <a href="/cookies" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent)', fontWeight: 600, textDecoration: 'none' }}>Política de Cookies</a>.
                </div>
              </div>
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
      <footer style={{ textAlign: 'center', padding: '16px 20px 32px', fontSize: '0.78rem', color: 'var(--muted)', width: '100%' }}>
        <a href="/termos" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--muted)', margin: '0 8px', textDecoration: 'none' }}>Termos de Uso</a> | 
        <a href="/privacidade" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--muted)', margin: '0 8px', textDecoration: 'none' }}>Política de Privacidade</a> | 
        <a href="/cookies" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--muted)', margin: '0 8px', textDecoration: 'none' }}>Cookies</a>
      </footer>
    </main>
  )
}

export default LoginPage

