import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import Button from './components/ui/Button'
import Input, { Textarea } from './components/ui/Input'

const INITIAL_FORM = { nome: '', telefone: '', areaAtuacao: '' }

function normalizePayload(data) {
  return {
    nome: data.nome,
    telefone: data.telefone,
    historicoServicos: data.areaAtuacao,
  }
}

function PrestadoresPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [editing, setEditing] = useState({})
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [pendingInactivateId, setPendingInactivateId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadPrestadores() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/prestadores')
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar prestadores.'))
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadPrestadores()
    }, 0)
    return () => clearTimeout(timer)
  }, [])

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function onEditChange(id, e) {
    setEditing((prev) => ({
      ...prev,
      [id]: { ...(prev[id] || {}), [e.target.name]: e.target.value },
    }))
  }

  function startEdit(p) {
    setEditing((prev) => ({
      ...prev,
      [p.id]: {
        nome: p.nome,
        telefone: p.telefone,
        areaAtuacao: p.historicoServicos || '',
      },
    }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const res = await apiFetch('/api/prestadores', {
        method: 'POST',
        body: JSON.stringify(normalizePayload(form)),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao cadastrar prestador.'))
      }
      setSuccess('Prestador cadastrado com sucesso.')
      setForm(INITIAL_FORM)
      await loadPrestadores()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function onUpdate(id) {
    const data = editing[id]
    if (!data?.nome?.trim() || !data?.telefone?.trim()) return
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/prestadores/${id}`, {
        method: 'PUT',
        body: JSON.stringify(normalizePayload(data)),
      })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao atualizar prestador.'))
      setSuccess('Prestador atualizado com sucesso.')
      setEditing((prev) => {
        const copy = { ...prev }
        delete copy[id]
        return copy
      })
      await loadPrestadores()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onInactivate(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/prestadores/${id}/inativar`, { method: 'POST' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao inativar prestador.'))
      setSuccess('Prestador inativado com sucesso.')
      setPendingInactivateId(null)
      await loadPrestadores()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Serviços terceirizados</p>
        <h1>Prestadores de serviço</h1>
        <p className="subtitle">Cadastre nome, telefone e área de atuação dos profissionais.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel section-spacer">
        <h2>Novo prestador</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Nome *<Input name="nome" value={form.nome} onChange={onChange} required maxLength={150} /></label>
          <label>Telefone *<Input name="telefone" value={form.telefone} onChange={onChange} required maxLength={30} placeholder="(11) 99999-0000" /></label>
          <label className="full">Área de atuação<Textarea name="areaAtuacao" value={form.areaAtuacao} onChange={onChange} rows={3} maxLength={4000} placeholder="Ex: Hidráulica, elétrica predial, manutenção de bombas." /></label>
          <div className="notice-box full">
            <strong>Aviso LGPD:</strong> Ao cadastrar dados de moradores, prestadores de serviço ou terceiros, declaro que possuo autorização, obrigação legal, relação contratual ou outra base legal adequada para realizar esse cadastro, responsabilizando-me pela exatidão das informações inseridas e pelo uso da plataforma conforme a LGPD.
          </div>
          <Button type="submit" disabled={submitting} className="full">
            {submitting ? 'Salvando...' : 'Cadastrar prestador'}
          </Button>
        </form>
      </section>

      <section className="board section-spacer">
        {loading ? <LoadingState message="Carregando prestadores..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={loadPrestadores} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhum prestador cadastrado." /> : null}
        {items.map((p) => (
          <article key={p.id} className="item prestador-item">
            {editing[p.id] ? (
              <>
                <label>Nome<Input name="nome" value={editing[p.id].nome} onChange={(e) => onEditChange(p.id, e)} /></label>
                <label>Telefone<Input name="telefone" value={editing[p.id].telefone} onChange={(e) => onEditChange(p.id, e)} /></label>
                <label>Área de atuação<Textarea name="areaAtuacao" value={editing[p.id].areaAtuacao} onChange={(e) => onEditChange(p.id, e)} rows={3} maxLength={4000} /></label>
                <div className="item-actions">
                  <Button onClick={() => onUpdate(p.id)}>Salvar</Button>
                  <Button variant="secondary" onClick={() => setEditing((prev) => { const c = { ...prev }; delete c[p.id]; return c })}>Cancelar</Button>
                </div>
              </>
            ) : (
              <>
                <h3>{p.nome}</h3>
                <p className="phone">{p.telefone}</p>
                {p.historicoServicos ? <p className="history">Área de atuação: {p.historicoServicos}</p> : <p className="muted">Área de atuação não informada.</p>}
                <div className="item-actions">
                  <Button onClick={() => startEdit(p)}>Editar</Button>
                  <Button variant="danger" onClick={() => setPendingInactivateId(p.id)}>Inativar</Button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>

      <ConfirmDialog
        open={pendingInactivateId != null}
        title="Inativar prestador"
        message="Deseja inativar este prestador? Ele deixará de aparecer na lista de prestadores ativos."
        confirmLabel="Inativar"
        onCancel={() => setPendingInactivateId(null)}
        onConfirm={() => onInactivate(pendingInactivateId)}
      />
    </>
  )
}

export default PrestadoresPage
