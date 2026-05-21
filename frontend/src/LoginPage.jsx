import { useState, useEffect, useRef } from 'react'
import { login, register, loginComGoogle } from './api'

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

  // Usamos Refs para evitar stale closures no callback assíncrono do Google Sign-In
  const aceitouTermosRef = useRef(aceitouTermos)
  const aceitouMarketingRef = useRef(aceitouMarketing)

  useEffect(() => {
    aceitouTermosRef.current = aceitouTermos
  }, [aceitouTermos])

  useEffect(() => {
    aceitouMarketingRef.current = aceitouMarketing
  }, [aceitouMarketing])

  // Efeito para carregar o Google Identity Services SDK dinamicamente
  useEffect(() => {
    const scriptId = 'google-gsi-client'
    let script = document.getElementById(scriptId)

    if (!script) {
      script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.id = scriptId
      script.async = true;
      script.defer = true;
      document.body.appendChild(script)
    }

    script.onload = () => {
      inicializarGoogleSignIn()
    }

    if (window.google) {
      inicializarGoogleSignIn()
    }

    function inicializarGoogleSignIn() {
      try {
        window.google.accounts.id.initialize({
          client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || '7569526-tfrrghq40es98m92r09f4m8i97e930u8.apps.googleusercontent.com',
          callback: handleCredentialResponse,
          auto_select: false,
          cancel_on_tap_outside: true
        })

        // Renderiza o botão oficial com visual neutro e adaptado
        window.google.accounts.id.renderButton(
          document.getElementById('google-signin-btn-container'),
          {
            theme: 'filled_blue',
            size: 'large',
            width: '100%',
            text: 'continue_with',
            shape: 'pill',
            locale: 'pt-BR'
          }
        )
      } catch (e) {
        console.error('Erro ao inicializar SDK do Google:', e)
      }
    }
  }, [])

  // Callback acionado quando o Google retorna o token de credencial
  async function handleCredentialResponse(response) {
    setError('')
    setSuccess('')
    
    // Validação estrita da LGPD no frontend antes de bater no backend
    if (!aceitouTermosRef.current) {
      setError('Você precisa aceitar os Termos de Uso e a Política de Privacidade antes de continuar com o Google.')
      return
    }

    setLoading(true)
    try {
      const user = await loginComGoogle({
        credentialToken: response.credential,
        aceitouTermos: aceitouTermosRef.current,
        aceitouMarketing: aceitouMarketingRef.current
      })
      setSuccess('Autenticação com o Google realizada com sucesso!')
      onLogin(user)
    } catch (err) {
      setError(err.message || 'Falha na autenticação com o Google.')
    } finally {
      setLoading(false)
    }
  }

  // Intercepta e barra o clique no Google se as políticas LGPD não forem aceitas
  function handleGoogleClickBlocked(e) {
    e.preventDefault()
    e.stopPropagation()
    setError('Por favor, declare o seu consentimento marcando o checkbox dos Termos de Uso e Política de Privacidade para habilitar a conexão com o Google.')
  }

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
        setSuccess('Cadastro realizado com sucesso! Entrando na sua conta...')
      }
      const user = await login(email, senha)
      onLogin(user)
    } catch (err) {
      setError(err.message || 'Credenciais inválidas')
    } finally {
      setLoading(false)
    }
  }

  const LogoSVG = () => (
    <svg className="auth-logo-svg" width="56" height="56" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="logo-grad-1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#2dd4bf" />
          <stop offset="100%" stopColor="#6366f1" />
        </linearGradient>
        <linearGradient id="logo-grad-2" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#818cf8" />
          <stop offset="100%" stopColor="#34d399" />
        </linearGradient>
      </defs>
      <path d="M18 30 L32 18 L46 30 L46 48 L18 48 Z" stroke="url(#logo-grad-1)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" opacity="0.6" />
      <path d="M26 36 L40 25 L54 36 L54 54 L26 54 Z" stroke="url(#logo-grad-2)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="32" cy="18" r="3.5" fill="#2dd4bf" />
      <circle cx="40" cy="25" r="3.5" fill="#6366f1" />
      <circle cx="26" cy="36" r="3.5" fill="#34d399" />
      <circle cx="46" cy="48" r="3" fill="#2dd4bf" opacity="0.8" />
      <circle cx="54" cy="54" r="3" fill="#6366f1" />
      <line x1="32" y1="18" x2="40" y2="25" stroke="#fff" strokeWidth="1.5" strokeDasharray="3 3" opacity="0.6" />
      <line x1="40" y1="25" x2="26" y2="36" stroke="#fff" strokeWidth="1.5" strokeDasharray="3 3" opacity="0.6" />
      <line x1="26" y1="36" x2="46" y2="48" stroke="#fff" strokeWidth="1.5" strokeDasharray="3 3" opacity="0.4" />
    </svg>
  )

  return (
    <main className="auth-page">

      {/* ── Painel esquerdo: Branding + Features ── */}
      <aside className="auth-left-panel">
        <div className="auth-glow-1" />
        <div className="auth-glow-2" />

        <header className="auth-brand-container">
          <LogoSVG />
          <h1 className="auth-brand">
            LiveSind<span className="brand-ia">IA</span>
          </h1>
          <p className="auth-subtitle">
            Gestão condominial inteligente com IA e total segurança de dados
          </p>
        </header>

        <ul className="auth-features-list">
          <li className="auth-feature-item">
            <div className="auth-feature-icon">🏢</div>
            <div className="auth-feature-text">
              <strong>Manutenções e Reuniões</strong>
              <span>Registre, acompanhe e gere atas automaticamente com IA.</span>
            </div>
          </li>
          <li className="auth-feature-item">
            <div className="auth-feature-icon">💰</div>
            <div className="auth-feature-text">
              <strong>Financeiro Integrado</strong>
              <span>Controle de gastos, prestadores e análise financeira com IA.</span>
            </div>
          </li>
          <li className="auth-feature-item">
            <div className="auth-feature-icon">🤖</div>
            <div className="auth-feature-text">
              <strong>Assistente IA com RAG</strong>
              <span>Tire dúvidas e tome decisões com dados reais do seu condomínio.</span>
            </div>
          </li>
          <li className="auth-feature-item">
            <div className="auth-feature-icon">🔒</div>
            <div className="auth-feature-text">
              <strong>Segurança e Privacidade</strong>
              <span>Dados isolados por condomínio, conformidade com LGPD.</span>
            </div>
          </li>
        </ul>

        <div className="auth-trust-bar">
          <div className="auth-trust-dots">
            <span className="auth-trust-dot" />
            <span className="auth-trust-dot" />
            <span className="auth-trust-dot" />
          </div>
          <div className="auth-trust-text">
            <strong>Confiança de síndicos em todo o Brasil</strong>
            Plataforma segura, moderna e inteligente
          </div>
        </div>
      </aside>

      {/* ── Painel direito: Formulário ── */}
      <div className="auth-right-panel">
        <article className="auth-glass-panel">
          <h2>{mode === 'login' ? 'Bem-vindo de volta' : 'Crie sua conta'}</h2>
          <p className="auth-panel-subtitle">
            {mode === 'login' ? 'Entre na sua conta para continuar' : 'Gerencie seu condomínio com inteligência'}
          </p>

        <div className="auth-toggle-bar" aria-label="Alternar entre login e cadastro">
          <button
            type="button"
            className={`auth-toggle-btn ${mode === 'login' ? 'auth-toggle-btn--active' : ''}`}
            onClick={() => {
              setMode('login')
              setError('')
              setSuccess('')
            }}
            disabled={loading}
          >
            Entrar
          </button>
          <button
            type="button"
            className={`auth-toggle-btn ${mode === 'register' ? 'auth-toggle-btn--active' : ''}`}
            onClick={() => {
              setMode('register')
              setError('')
              setSuccess('')
            }}
            disabled={loading}
          >
            Cadastrar
          </button>
        </div>

        <form onSubmit={onSubmit} className="auth-form">
          {mode === 'register' && (
            <>
              <div className="auth-input-group">
                <label className="auth-input-label">Nome do síndico</label>
                <input
                  className="auth-input-field"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  required
                  maxLength={150}
                  placeholder="Seu nome completo"
                  disabled={loading}
                />
              </div>
              <div className="auth-input-group">
                <label className="auth-input-label">Nome do condomínio</label>
                <input
                  className="auth-input-field"
                  value={nomeCondominio}
                  onChange={(e) => setNomeCondominio(e.target.value)}
                  required
                  maxLength={150}
                  placeholder="Ex: Condomínio Residencial Flores"
                  disabled={loading}
                />
              </div>
            </>
          )}

          <div className="auth-input-group">
            <label className="auth-input-label">E-mail</label>
            <input
              type="email"
              className="auth-input-field"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              placeholder="sindico@demo.local"
              disabled={loading}
            />
          </div>

          <div className="auth-input-group">
            <div className="auth-input-label">
              <span>Senha</span>
              {mode === 'login' && <a href="/esqueci-senha">Esqueci minha senha</a>}
            </div>
            <input
              type="password"
              className="auth-input-field"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              placeholder="••••••••"
              disabled={loading}
            />
          </div>

          {mode === 'register' && (
            <div className="auth-input-group">
              <label className="auth-input-label">Confirmar senha</label>
              <input
                type="password"
                className="auth-input-field"
                value={confirmarSenha}
                onChange={(e) => setConfirmarSenha(e.target.value)}
                required
                autoComplete="new-password"
                placeholder="••••••••"
                disabled={loading}
              />
            </div>
          )}

          {/* Seção LGPD: Sempre mostramos checkboxes para novos cadastros (seja clássico ou Google) */}
          {(mode === 'register' || mode === 'login') && (
            <div className="auth-lgpd-container">
              {mode === 'login' && (
                <div style={{ fontSize: '0.78rem', color: '#9ca3af', marginBottom: '4px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.02em' }}>
                  Acesso Social (Google)
                </div>
              )}
              
              <label className="auth-lgpd-label">
                <input
                  type="checkbox"
                  checked={aceitouTermos}
                  onChange={(e) => setAceitouTermos(e.target.checked)}
                  required={mode === 'register'}
                  disabled={loading}
                />
                <span>
                  Declaro que li e aceito os <a href="/termos" target="_blank" rel="noopener noreferrer">Termos de Uso</a> e a <a href="/privacidade" target="_blank" rel="noopener noreferrer">Política de Privacidade</a> da plataforma, autorizando o tratamento dos meus dados.
                </span>
              </label>

              <label className="auth-lgpd-label">
                <input
                  type="checkbox"
                  checked={aceitouMarketing}
                  onChange={(e) => setAceitouMarketing(e.target.checked)}
                  disabled={loading}
                />
                <span>
                  Aceito receber comunicações comerciais, atualizações do sistema e ofertas exclusivas da plataforma.
                </span>
              </label>

              {mode === 'login' && (
                <div style={{ fontSize: '0.75rem', color: '#6b7280', borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: '8px', marginTop: '2px' }}>
                  Nota: Marque o primeiro checkbox acima para habilitar o login social com o Google de forma segura.
                </div>
              )}
            </div>
          )}

          {error && <p className="message error" style={{ margin: '0 0 16px 0', borderRadius: '10px' }}>{error}</p>}
          {success && <p className="message success" style={{ margin: '0 0 16px 0', borderRadius: '10px' }}>{success}</p>}

          <button type="submit" disabled={loading} className="auth-submit-btn">
            {loading ? 'Processando...' : mode === 'login' ? 'Entrar com Email' : 'Cadastrar Conta'}
          </button>
        </form>

        {/* Divisor e seção de Login Social */}
        <div className="auth-divider">ou</div>

        <div style={{ position: 'relative', width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          {/* Container onde o Google Identity Services renderizará o botão oficial */}
          <div id="google-signin-btn-container" style={{ width: '100%', minHeight: '44px' }}></div>
          
          {/* Overlay transparente que barra o clique caso a LGPD não seja consentida */}
          {!aceitouTermos && (
            <div
              onClick={handleGoogleClickBlocked}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                zIndex: 20,
                cursor: 'pointer',
                background: 'transparent',
                borderRadius: '9999px'
              }}
              title="Aceite os Termos e Políticas acima para habilitar o botão do Google"
            />
          )}
        </div>

        <p className="auth-footer-text">
          {mode === 'login' ? 'Novo na plataforma? ' : 'Já possui uma conta? '}
          <button
            type="button"
            onClick={() => {
              setMode(mode === 'login' ? 'register' : 'login')
              setError('')
              setSuccess('')
            }}
            disabled={loading}
          >
            {mode === 'login' ? 'Crie uma conta grátis' : 'Acesse agora'}
          </button>
        </p>
      </article>

      <footer className="auth-global-footer">
        <a href="/termos" target="_blank" rel="noopener noreferrer">Termos de Uso</a> |
        <a href="/privacidade" target="_blank" rel="noopener noreferrer">Diretrizes de Privacidade</a> |
        <a href="/cookies" target="_blank" rel="noopener noreferrer">Gestão de Cookies</a>
      </footer>
      </div>

    </main>
  )
}

export default LoginPage
