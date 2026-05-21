import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'

const PAPEIS = ['PROPRIETARIO', 'INQUILINO', 'DEPENDENTE', 'ZELADOR', 'OUTRO']

const INITIAL_UNIDADE = { bloco: '', numero: '', complemento: '' }
const INITIAL_MORADOR = { unidadeId: '', nome: '', email: '', telefone: '', papel: 'PROPRIETARIO', observacoes: '' }

function MoradoresPage() {
  const [unidades, setUnidades] = useState([])
  const [moradores, setMoradores] = useState([])
  const [loading, setLoading] = useState(true)
  const [formUnidade, setFormUnidade] = useState(INITIAL_UNIDADE)
  const [formMorador, setFormMorador] = useState(INITIAL_MORADOR)
  const [submittingUnidade, setSubmittingUnidade] = useState(false)
  const [submittingMorador, setSubmittingMorador] = useState(false)
  const [editingMorador, setEditingMorador] = useState({})
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [uRes, mRes] = await Promise.all([
        apiFetch('/api/unidades'),
        apiFetch('/api/moradores'),
      ])
      if (!uRes.ok) throw new Error(await parseError(uRes, 'Falha ao carregar unidades.'))
      if (!mRes.ok) throw new Error(await parseError(mRes, 'Falha ao carregar moradores.'))
      const [u, m] = await Promise.all([parseJson(uRes), parseJson(mRes)])
      setUnidades(u)
      setMoradores(m)
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

  function onMoradorChange(e) {
    setFormMorador((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function onEditChange(id, e) {
    setEditingMorador((prev) => ({
      ...prev,
      [id]: { ...(prev[id] || {}), [e.target.name]: e.target.value },
    }))
  }

  function startEditMorador(m) {
    setEditingMorador((prev) => ({
      ...prev,
      [m.id]: {
        unidadeId: m.unidadeId,
        nome: m.nome,
        email: m.email || '',
        telefone: m.telefone || '',
        papel: m.papel,
        observacoes: m.observacoes || '',
      },
    }))
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
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmittingUnidade(false)
    }
  }

  async function onSubmitMorador(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmittingMorador(true)
    try {
      const res = await apiFetch('/api/moradores', {
        method: 'POST',
        body: JSON.stringify({ ...formMorador, unidadeId: formMorador.unidadeId || null }),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao cadastrar morador.'))
      }
      setSuccess('Morador cadastrado com sucesso.')
      setFormMorador(INITIAL_MORADOR)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmittingMorador(false)
    }
  }

  async function onUpdateMorador(id) {
    const data = editingMorador[id]
    if (!data?.nome?.trim()) return
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/moradores/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...data, unidadeId: data.unidadeId || null }),
      })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao atualizar morador.'))
      setSuccess('Morador atualizado com sucesso.')
      setEditingMorador((prev) => { const c = { ...prev }; delete c[id]; return c })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onInativar(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/moradores/${id}/inativar`, { method: 'POST' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao inativar morador.'))
      setSuccess('Morador inativado com sucesso.')
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

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginTop: 20 }}>
        <section className="panel">
          <h2>Nova unidade</h2>
          <form onSubmit={onSubmitUnidade} className="form-grid">
            <label>Bloco<input name="bloco" value={formUnidade.bloco} onChange={onUnidadeChange} maxLength={30} placeholder="Ex: A, Torre 1..." /></label>
            <label>Número *<input name="numero" value={formUnidade.numero} onChange={onUnidadeChange} required maxLength={30} /></label>
            <label className="full">Complemento<input name="complemento" value={formUnidade.complemento} onChange={onUnidadeChange} maxLength={100} /></label>
            <button type="submit" disabled={submittingUnidade} className="submit full">
              {submittingUnidade ? 'Salvando...' : 'Cadastrar unidade'}
            </button>
          </form>
        </section>

        <section className="panel">
          <h2>Novo morador</h2>
          <form onSubmit={onSubmitMorador} className="form-grid">
            <label className="full">
              Unidade *
              <select name="unidadeId" value={formMorador.unidadeId} onChange={onMoradorChange} required>
                <option value="">Selecione...</option>
                {unidades.map((u) => <option key={u.id} value={u.id}>{u.rotulo}</option>)}
              </select>
            </label>
            <label>Nome *<input name="nome" value={formMorador.nome} onChange={onMoradorChange} required maxLength={150} /></label>
            <label>
              Papel
              <select name="papel" value={formMorador.papel} onChange={onMoradorChange}>
                {PAPEIS.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </label>
            <label>Email<input name="email" type="email" value={formMorador.email} onChange={onMoradorChange} maxLength={150} /></label>
            <label>Telefone<input name="telefone" value={formMorador.telefone} onChange={onMoradorChange} maxLength={30} /></label>
            <label className="full">Observações<textarea name="observacoes" value={formMorador.observacoes} onChange={onMoradorChange} rows={2} /></label>
            <button type="submit" disabled={submittingMorador} className="submit full">
              {submittingMorador ? 'Salvando...' : 'Cadastrar morador'}
            </button>
          </form>
        </section>
      </div>

      <section className="board" style={{ marginTop: 24 }}>
        <h2 style={{ marginBottom: 12 }}>Moradores ativos</h2>
        {loading ? <LoadingState message="Carregando moradores..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && moradores.length === 0 ? <EmptyState message="Nenhum morador cadastrado." /> : null}
        {moradores.map((m) => (
          <article key={m.id} className="item">
            {editingMorador[m.id] ? (
              <>
                <label className="full">
                  Unidade
                  <select name="unidadeId" value={editingMorador[m.id].unidadeId} onChange={(e) => onEditChange(m.id, e)}>
                    {unidades.map((u) => <option key={u.id} value={u.id}>{u.rotulo}</option>)}
                  </select>
                </label>
                <label>Nome<input name="nome" value={editingMorador[m.id].nome} onChange={(e) => onEditChange(m.id, e)} /></label>
                <label>
                  Papel
                  <select name="papel" value={editingMorador[m.id].papel} onChange={(e) => onEditChange(m.id, e)}>
                    {PAPEIS.map((p) => <option key={p} value={p}>{p}</option>)}
                  </select>
                </label>
                <label>Email<input name="email" type="email" value={editingMorador[m.id].email} onChange={(e) => onEditChange(m.id, e)} /></label>
                <label>Telefone<input name="telefone" value={editingMorador[m.id].telefone} onChange={(e) => onEditChange(m.id, e)} /></label>
                <label className="full">Observações<textarea name="observacoes" value={editingMorador[m.id].observacoes} onChange={(e) => onEditChange(m.id, e)} rows={2} /></label>
                <div className="item-actions">
                  <button className="submit" style={{ flex: 1 }} onClick={() => onUpdateMorador(m.id)}>Salvar</button>
                  <button className="submit cancel" onClick={() => setEditingMorador((prev) => { const c = { ...prev }; delete c[m.id]; return c })}>Cancelar</button>
                </div>
              </>
            ) : (
              <>
                <h3>{m.nome} <small className="muted" style={{ fontWeight: 400 }}>· {m.unidadeRotulo}</small></h3>
                <p className="muted" style={{ marginTop: 2 }}>{m.papel}{m.telefone ? ` · ${m.telefone}` : ''}{m.email ? ` · ${m.email}` : ''}</p>
                {m.observacoes ? <p style={{ marginTop: 4 }}>{m.observacoes}</p> : null}
                <div className="item-actions">
                  <button className="submit" onClick={() => startEditMorador(m)}>Editar</button>
                  <button className="submit danger" onClick={() => onInativar(m.id)}>Inativar</button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>
    </>
  )
}

export default MoradoresPage
