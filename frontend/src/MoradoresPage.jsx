import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import MoradorForm from './components/MoradorForm'
import Button from './components/ui/Button'
import Input from './components/ui/Input'

const INITIAL_UNIDADE = { bloco: '', numero: '', complemento: '' }

function MoradoresPage() {
  const formRef = useRef(null)
  const [moradores, setMoradores] = useState([])
  const [loading, setLoading] = useState(true)
  const [formUnidade, setFormUnidade] = useState(INITIAL_UNIDADE)
  const [submittingUnidade, setSubmittingUnidade] = useState(false)
  // Incrementado ao cadastrar unidade, para o formulário de morador recarregar as unidades
  const [unidadesVersion, setUnidadesVersion] = useState(0)
  const [editingMorador, setEditingMorador] = useState(null)
  const [pendingInativarId, setPendingInativarId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/moradores')
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar moradores.'))
      setMoradores(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void load()
    }, 0)
    return () => clearTimeout(timer)
  }, [])

  function onUnidadeChange(e) {
    setFormUnidade((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function startEditMorador(morador) {
    setError('')
    setSuccess('')
    setEditingMorador(morador)
    window.setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  async function onSubmitUnidade(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmittingUnidade(true)
    try {
      const res = await apiFetch('/api/unidades', {
        method: 'POST',
        body: JSON.stringify(formUnidade),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao cadastrar unidade.'))
      }
      setSuccess('Unidade cadastrada com sucesso.')
      setFormUnidade(INITIAL_UNIDADE)
      setUnidadesVersion((v) => v + 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmittingUnidade(false)
    }
  }

  async function onMoradorSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Morador atualizado com sucesso.' : 'Morador cadastrado com sucesso.')
    setEditingMorador(null)
    await load()
  }

  async function onInativar(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/moradores/${id}/inativar`, { method: 'POST' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao inativar morador.'))
      setSuccess('Morador inativado com sucesso.')
      setPendingInativarId(null)
      if (editingMorador?.id === id) setEditingMorador(null)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Cadastro de moradores</p>
        <h1>Unidades e Moradores</h1>
        <p className="subtitle">Gerencie as unidades e os moradores do condomínio.</p>
      </section>

      <SuccessState message={success} />

      <div className="two-panel-grid">
        <section className="panel">
          <h2>Nova unidade</h2>
          <form onSubmit={onSubmitUnidade} className="form-grid">
            <label>Bloco<Input name="bloco" value={formUnidade.bloco} onChange={onUnidadeChange} maxLength={30} placeholder="Ex: A, Torre 1..." /></label>
            <label>Número *<Input name="numero" value={formUnidade.numero} onChange={onUnidadeChange} required maxLength={30} /></label>
            <label className="full">Complemento<Input name="complemento" value={formUnidade.complemento} onChange={onUnidadeChange} maxLength={100} /></label>
            <Button type="submit" disabled={submittingUnidade} className="full">
              {submittingUnidade ? 'Salvando...' : 'Cadastrar unidade'}
            </Button>
          </form>
        </section>

        <section className="panel" ref={formRef}>
          <h2>{editingMorador ? 'Editar morador' : 'Novo morador'}</h2>
          <MoradorForm
            key={`${editingMorador?.id ?? 'novo'}-${unidadesVersion}`}
            item={editingMorador}
            onSaved={onMoradorSaved}
            onCancel={editingMorador ? () => setEditingMorador(null) : undefined}
          />
        </section>
      </div>

      <section className="board section-spacer">
        <h2 className="board-title">Moradores ativos</h2>
        {loading ? <LoadingState message="Carregando moradores..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && moradores.length === 0 ? <EmptyState message="Nenhum morador cadastrado." /> : null}
        {moradores.map((m) => (
          <article key={m.id} className="item">
            <h3 className="item-title">{m.nome} <small className="muted">· {m.unidadeRotulo}</small></h3>
            <p className="muted item-meta">{m.papel}{m.telefone ? ` · ${m.telefone}` : ''}{m.email ? ` · ${m.email}` : ''}</p>
            {m.observacoes ? <p className="item-description">{m.observacoes}</p> : null}
            <div className="item-actions">
              <Button onClick={() => startEditMorador(m)}>Editar</Button>
              <Button variant="danger" onClick={() => setPendingInativarId(m.id)}>Inativar</Button>
            </div>
          </article>
        ))}
      </section>

      <ConfirmDialog
        open={pendingInativarId != null}
        title="Inativar morador"
        message="Deseja inativar este morador? Ele deixará de aparecer na lista de moradores ativos."
        confirmLabel="Inativar"
        onCancel={() => setPendingInativarId(null)}
        onConfirm={() => onInativar(pendingInativarId)}
      />
    </>
  )
}

export default MoradoresPage
