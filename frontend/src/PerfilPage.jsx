import { useEffect, useState } from 'react'
import { getPerfil, salvarDadosPerfil, trocarSenha } from './api'
import { ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import Alert from './components/ui/Alert'
import Button from './components/ui/Button'
import Input from './components/ui/Input'

const SENHA_VAZIA = { senhaAtual: '', novaSenha: '', confirmarSenha: '' }

function PerfilPage() {
  const [perfil, setPerfil] = useState(null)
  const [dados, setDados] = useState({ nome: '', telefone: '' })
  const [senha, setSenha] = useState(SENHA_VAZIA)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [savingDados, setSavingDados] = useState(false)
  const [savingSenha, setSavingSenha] = useState(false)
  const [errorDados, setErrorDados] = useState('')
  const [errorSenha, setErrorSenha] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let ativo = true
    getPerfil()
      .then((data) => {
        if (!ativo) return
        setPerfil(data)
        setDados({ nome: data.nome || '', telefone: data.telefone || '' })
      })
      .catch((err) => {
        if (ativo) setLoadError(err.message)
      })
      .finally(() => {
        if (ativo) setLoading(false)
      })
    return () => {
      ativo = false
    }
  }, [])

  async function onSubmitDados(event) {
    event.preventDefault()
    setErrorDados('')
    setSuccess('')
    setSavingDados(true)
    try {
      const atualizado = await salvarDadosPerfil(dados)
      setPerfil(atualizado)
      setDados({ nome: atualizado.nome || '', telefone: atualizado.telefone || '' })
      setSuccess('Dados atualizados com sucesso.')
    } catch (err) {
      setErrorDados(err.message)
    } finally {
      setSavingDados(false)
    }
  }

  async function onSubmitSenha(event) {
    event.preventDefault()
    setErrorSenha('')
    setSuccess('')
    setSavingSenha(true)
    try {
      await trocarSenha(senha)
      setSenha(SENHA_VAZIA)
      setSuccess('Senha alterada com sucesso.')
    } catch (err) {
      setErrorSenha(err.message)
    } finally {
      setSavingSenha(false)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Conta</p>
        <h1>Meu perfil</h1>
        <p className="subtitle">Atualize seus dados e a senha de acesso.</p>
      </section>

      <SuccessState message={success} />

      {loading ? <LoadingState message="Carregando perfil..." /> : null}
      {!loading && loadError ? <ErrorState message={loadError} /> : null}

      {!loading && !loadError && perfil ? (
        <>
          <section className="panel section-spacer">
            <h2>Dados pessoais</h2>
            <form onSubmit={onSubmitDados} className="form-grid">
              <label>
                E-mail
                <Input value={perfil.email || ''} disabled readOnly />
              </label>
              <label>
                Nome *
                <Input
                  name="nome"
                  value={dados.nome}
                  onChange={(event) => setDados((prev) => ({ ...prev, nome: event.target.value }))}
                  required
                  maxLength={150}
                />
              </label>
              <label className="full">
                Telefone
                <Input
                  name="telefone"
                  value={dados.telefone}
                  onChange={(event) => setDados((prev) => ({ ...prev, telefone: event.target.value }))}
                  maxLength={30}
                />
              </label>
              {errorDados ? <Alert variant="error" role="alert" className="full">{errorDados}</Alert> : null}
              <div className="full">
                <Button type="submit" disabled={savingDados}>
                  {savingDados ? 'Salvando...' : 'Salvar dados'}
                </Button>
              </div>
            </form>
          </section>

          <section className="panel section-spacer">
            <h2>Alterar senha</h2>
            <form onSubmit={onSubmitSenha} className="form-grid">
              <label className="full">
                Senha atual
                <Input
                  type="password"
                  name="senhaAtual"
                  value={senha.senhaAtual}
                  onChange={(event) => setSenha((prev) => ({ ...prev, senhaAtual: event.target.value }))}
                  required
                  autoComplete="current-password"
                />
              </label>
              <label>
                Nova senha
                <Input
                  type="password"
                  name="novaSenha"
                  value={senha.novaSenha}
                  onChange={(event) => setSenha((prev) => ({ ...prev, novaSenha: event.target.value }))}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </label>
              <label>
                Confirmar nova senha
                <Input
                  type="password"
                  name="confirmarSenha"
                  value={senha.confirmarSenha}
                  onChange={(event) => setSenha((prev) => ({ ...prev, confirmarSenha: event.target.value }))}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </label>
              {errorSenha ? <Alert variant="error" role="alert" className="full">{errorSenha}</Alert> : null}
              <div className="full">
                <Button type="submit" disabled={savingSenha}>
                  {savingSenha ? 'Alterando...' : 'Alterar senha'}
                </Button>
              </div>
            </form>
          </section>
        </>
      ) : null}
    </>
  )
}

export default PerfilPage
