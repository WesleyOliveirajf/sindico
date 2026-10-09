import { useState } from 'react'
import { solicitarResetSenha } from './api'
import Alert from './components/ui/Alert'
import Button from './components/ui/Button'
import Input from './components/ui/Input'

function EsqueciSenhaPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState('')

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await solicitarResetSenha(email.trim())
      setEnviado(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <section className="panel" style={{ maxWidth: 440, margin: '48px auto' }}>
        <p className="eyebrow">Recuperar acesso</p>
        <h1>Esqueci minha senha</h1>

        {enviado ? (
          <Alert variant="success" role="status">
            Se este e-mail estiver cadastrado, você receberá um link para redefinir sua senha em breve.
          </Alert>
        ) : (
          <form onSubmit={onSubmit} className="form-grid">
            <label className="full">
              E-mail
              <Input
                type="email"
                name="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                autoComplete="email"
              />
            </label>
            {error ? <Alert variant="error" role="alert" className="full">{error}</Alert> : null}
            <div className="full">
              <Button type="submit" disabled={loading}>
                {loading ? 'Enviando...' : 'Enviar link'}
              </Button>
            </div>
          </form>
        )}

        <p style={{ marginTop: 16 }}>
          <a href="/">Voltar ao login</a>
        </p>
      </section>
    </main>
  )
}

export default EsqueciSenhaPage
