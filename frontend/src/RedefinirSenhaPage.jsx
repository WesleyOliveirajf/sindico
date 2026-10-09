import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { redefinirSenha, validarTokenReset } from './api'
import Alert from './components/ui/Alert'
import Button from './components/ui/Button'
import Input from './components/ui/Input'

const MENSAGEM_TOKEN_AUSENTE = 'Link inválido ou expirado. Solicite um novo.'

function RedefinirSenhaPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''
  // Sem token nao ha o que validar: o estado inicial ja e "invalido".
  const [status, setStatus] = useState(token ? 'validando' : 'invalido')
  const [error, setError] = useState(token ? '' : MENSAGEM_TOKEN_AUSENTE)
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmarSenha, setConfirmarSenha] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!token) return undefined
    let ativo = true
    validarTokenReset(token)
      .then(() => {
        if (ativo) setStatus('pronto')
      })
      .catch((err) => {
        if (!ativo) return
        setError(err.message)
        setStatus('invalido')
      })
    return () => {
      ativo = false
    }
  }, [token])

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await redefinirSenha({ token, novaSenha, confirmarSenha })
      setStatus('concluido')
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
        <h1>Redefinir senha</h1>

        {status === 'validando' ? <p className="muted">Validando o link...</p> : null}

        {status === 'invalido' ? (
          <>
            <Alert variant="error" role="alert">{error}</Alert>
            <p style={{ marginTop: 16 }}>
              <a href="/esqueci-senha">Solicitar novo link</a>
            </p>
          </>
        ) : null}

        {status === 'pronto' ? (
          <form onSubmit={onSubmit} className="form-grid">
            <label className="full">
              Nova senha
              <Input
                type="password"
                name="novaSenha"
                value={novaSenha}
                onChange={(event) => setNovaSenha(event.target.value)}
                required
                minLength={8}
                autoComplete="new-password"
              />
            </label>
            <label className="full">
              Confirmar nova senha
              <Input
                type="password"
                name="confirmarSenha"
                value={confirmarSenha}
                onChange={(event) => setConfirmarSenha(event.target.value)}
                required
                minLength={8}
                autoComplete="new-password"
              />
            </label>
            {error ? <Alert variant="error" role="alert" className="full">{error}</Alert> : null}
            <div className="full">
              <Button type="submit" disabled={loading}>
                {loading ? 'Salvando...' : 'Redefinir senha'}
              </Button>
            </div>
          </form>
        ) : null}

        {status === 'concluido' ? (
          <>
            <Alert variant="success" role="status">
              Senha redefinida com sucesso! Faça login com a nova senha.
            </Alert>
            <p style={{ marginTop: 16 }}>
              <a href="/">Ir para o login</a>
            </p>
          </>
        ) : null}
      </section>
    </main>
  )
}

export default RedefinirSenhaPage
