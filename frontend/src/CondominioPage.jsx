import { useEffect, useState } from 'react'
import {
  getCondominio,
  getCondominioSelecionado,
  listarCondominios,
  salvarCondominio,
  setCondominioSelecionado,
} from './api'
import { ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import Alert from './components/ui/Alert'
import Button from './components/ui/Button'
import Input, { Select } from './components/ui/Input'

const FORM_VAZIO = { nome: '', cnpj: '', endereco: '' }

function CondominioPage() {
  const [form, setForm] = useState(FORM_VAZIO)
  const [condominios, setCondominios] = useState([])
  const [selecionadoId, setSelecionadoId] = useState(getCondominioSelecionado() || '')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let ativo = true
    Promise.all([getCondominio(), listarCondominios().catch(() => [])])
      .then(([dados, lista]) => {
        if (!ativo) return
        setForm({ nome: dados.nome || '', cnpj: dados.cnpj || '', endereco: dados.endereco || '' })
        setCondominios(lista)
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

  function onChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const atualizado = await salvarCondominio(form)
      setForm({
        nome: atualizado.nome || '',
        cnpj: atualizado.cnpj || '',
        endereco: atualizado.endereco || '',
      })
      setSuccess('Condomínio atualizado com sucesso.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  function onTrocarCondominio(event) {
    const id = event.target.value
    setSelecionadoId(id)
    setCondominioSelecionado(id)
    // Recarrega para que todas as telas busquem dados do novo condominio.
    window.location.reload()
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Cadastro</p>
        <h1>Condomínio</h1>
        <p className="subtitle">Dados do condomínio exibidos no sistema.</p>
      </section>

      <SuccessState message={success} />

      {condominios.length > 1 ? (
        <section className="panel section-spacer">
          <h2>Condomínio ativo</h2>
          <label>
            Você tem acesso a mais de um condomínio. Escolha o que deseja gerenciar.
            <Select value={selecionadoId} onChange={onTrocarCondominio}>
              {condominios.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </Select>
          </label>
        </section>
      ) : null}

      <section className="panel section-spacer">
        <h2>Dados do condomínio</h2>
        {loading ? <LoadingState message="Carregando condomínio..." /> : null}
        {!loading && loadError ? <ErrorState message={loadError} /> : null}

        {!loading && !loadError ? (
          <form onSubmit={onSubmit} className="form-grid">
            <label>
              Nome *
              <Input name="nome" value={form.nome} onChange={onChange} required minLength={3} maxLength={150} />
            </label>
            <label>
              CNPJ
              <Input name="cnpj" value={form.cnpj} onChange={onChange} maxLength={18} placeholder="00.000.000/0000-00" />
            </label>
            <label className="full">
              Endereço
              <Input name="endereco" value={form.endereco} onChange={onChange} maxLength={500} />
            </label>
            {error ? <Alert variant="error" role="alert" className="full">{error}</Alert> : null}
            <div className="full">
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Salvando...' : 'Salvar'}
              </Button>
            </div>
          </form>
        ) : null}
      </section>
    </>
  )
}

export default CondominioPage
