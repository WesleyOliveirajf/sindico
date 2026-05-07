import { useEffect, useState } from 'react'
import { parseJson } from './api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const INITIAL_FORM = { nome: '', telefone: '', historicoServicos: '' }

function PrestadoresPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [editing, setEditing] = useState({})
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadPrestadores() {
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`${API_BASE_URL}/api/prestadores`)
      if (!res.ok) throw new Error('Falha ao carregar prestadores.')
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadPrestadores() }, [])

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
      [p.id]: { nome: p.nome, telefone: p.telefone, historicoServicos: p.historicoServicos || '' },
    }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const res = await fetch(`${API_BASE_URL}/api/prestadores`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || 'Erro ao cadastrar prestador.')
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
      const res = await fetch(`${API_BASE_URL}/api/prestadores/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Erro ao atualizar prestador.')
      setSuccess('Prestador atualizado com sucesso.')
      setEditing((prev) => { const c = { ...prev }; delete c[id]; return c })
      await loadPrestadores()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onInactivate(id) {
    setError('')
    setSuccess('')
    try {
      const res = await fetch(`${API_BASE_URL}/api/prestadores/${id}/inativar`, { method: 'POST' })
      if (!res.ok) throw new Error('Erro ao inativar prestador.')
      setSuccess('Prestador inativado com sucesso.')
      await loadPrestadores()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Servicos terceirizados</p>
        <h1>Prestadores de servico</h1>
        <p className="subtitle">Cadastre contatos e mantenha historico de servicos realizados no condominio.</p>
      </section>

      {error ? <p className="message error">{error}</p> : null}
      {success ? <p className="message success">{success}</p> : null}

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Novo prestador</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Nome *<input name="nome" value={form.nome} onChange={onChange} required maxLength={150} /></label>
          <label>Telefone *<input name="telefone" value={form.telefone} onChange={onChange} required maxLength={30} placeholder="(11) 99999-0000" /></label>
          <label className="full">Historico de servicos<textarea name="historicoServicos" value={form.historicoServicos} onChange={onChange} rows={3} placeholder="Ex: 02/03 Troca de bomba. 18/04 Reparou portao." /></label>
          <button type="submit" disabled={submitting} className="submit full">
            {submitting ? 'Salvando...' : 'Cadastrar prestador'}
          </button>
        </form>
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <p className="muted">Carregando...</p> : null}
        {!loading && items.length === 0 ? <p className="muted">Nenhum prestador cadastrado.</p> : null}
        {items.map((p) => (
          <article key={p.id} className="item prestador-item">
            {editing[p.id] ? (
              <>
                <label>Nome<input name="nome" value={editing[p.id].nome} onChange={(e) => onEditChange(p.id, e)} /></label>
                <label>Telefone<input name="telefone" value={editing[p.id].telefone} onChange={(e) => onEditChange(p.id, e)} /></label>
                <label>Historico<textarea name="historicoServicos" value={editing[p.id].historicoServicos} onChange={(e) => onEditChange(p.id, e)} rows={3} /></label>
                <div className="item-actions">
                  <button className="submit" style={{ flex: 1 }} onClick={() => onUpdate(p.id)}>Salvar</button>
                  <button className="submit cancel" onClick={() => setEditing((prev) => { const c = { ...prev }; delete c[p.id]; return c })}>Cancelar</button>
                </div>
              </>
            ) : (
              <>
                <h3>{p.nome}</h3>
                <p className="phone">{p.telefone}</p>
                {p.historicoServicos ? <p className="history">{p.historicoServicos}</p> : <p className="muted">Sem historico registrado.</p>}
                <div className="item-actions">
                  <button className="submit" onClick={() => startEdit(p)}>Editar</button>
                  <button className="submit danger" onClick={() => onInactivate(p.id)}>Inativar</button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>
    </>
  )
}

export default PrestadoresPage
