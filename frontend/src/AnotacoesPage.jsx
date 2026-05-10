import { useEffect, useState } from 'react'
import { apiFetch, parseJson } from './api'

const IMPORTANCIAS = ['NORMAL', 'IMPORTANTE', 'CRITICO']

const INITIAL_FORM = { titulo: '', categoria: '', descricao: '', referencia: '', importancia: 'NORMAL' }
const INITIAL_FILTERS = { texto: '', dataInicio: '', dataFim: '' }

function AnotacoesPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [editing, setEditing] = useState({})
  const [items, setItems] = useState([])
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load(activeFilters = filters) {
    setLoading(true)
    setError('')
    try {
      const params = new URLSearchParams()
      if (activeFilters.texto.trim()) params.set('texto', activeFilters.texto.trim())
      if (activeFilters.dataInicio) params.set('dataInicio', activeFilters.dataInicio)
      if (activeFilters.dataFim) params.set('dataFim', activeFilters.dataFim)
      const qs = params.toString()
      const res = await apiFetch(`/api/anotacoes${qs ? `?${qs}` : ''}`)
      if (!res.ok) throw new Error('Falha ao carregar anotacoes.')
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function onFilterChange(e) {
    setFilters((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function onApplyFilters(e) {
    e.preventDefault()
    await load()
  }

  async function onClearFilters() {
    setFilters(INITIAL_FILTERS)
    await load(INITIAL_FILTERS)
  }

  function onEditChange(id, e) {
    setEditing((prev) => ({
      ...prev,
      [id]: { ...(prev[id] || {}), [e.target.name]: e.target.value },
    }))
  }

  function startEdit(a) {
    setEditing((prev) => ({
      ...prev,
      [a.id]: {
        titulo: a.titulo,
        categoria: a.categoria || '',
        descricao: a.descricao || '',
        referencia: a.referencia || '',
        importancia: a.importancia,
      },
    }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const res = await apiFetch('/api/anotacoes', {
        method: 'POST',
        body: JSON.stringify(form),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || 'Erro ao registrar anotacao.')
      }
      setSuccess('Anotacao registrada com sucesso.')
      setForm(INITIAL_FORM)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function onUpdate(id) {
    const data = editing[id]
    if (!data?.titulo?.trim()) return
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/anotacoes/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Erro ao atualizar anotacao.')
      setSuccess('Anotacao atualizada com sucesso.')
      setEditing((prev) => { const c = { ...prev }; delete c[id]; return c })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onDelete(id) {
    if (!confirm('Deseja excluir esta anotacao? Esta acao nao pode ser desfeita.')) return
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/anotacoes/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Erro ao excluir anotacao.')
      setSuccess('Anotacao excluida com sucesso.')
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  const importanciaClass = (i) => i === 'CRITICO' ? 'badge badge--critico' : i === 'IMPORTANTE' ? 'badge badge--importante' : 'badge'

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Registro de ocorrencias</p>
        <h1>Anotacoes</h1>
        <p className="subtitle">Registre informacoes, observacoes e ocorrencias relevantes do condominio.</p>
      </section>

      {error ? <p className="message error">{error}</p> : null}
      {success ? <p className="message success">{success}</p> : null}

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Filtros de busca</h2>
        <form onSubmit={onApplyFilters} className="form-grid">
          <label className="full">
            Buscar por titulo, categoria, descricao ou referencia
            <input
              name="texto"
              value={filters.texto}
              onChange={onFilterChange}
              placeholder="Ex: reuniao, vazamento, orcamento"
              maxLength={200}
            />
          </label>
          <label>
            Data inicial
            <input type="date" name="dataInicio" value={filters.dataInicio} onChange={onFilterChange} />
          </label>
          <label>
            Data final
            <input type="date" name="dataFim" value={filters.dataFim} onChange={onFilterChange} />
          </label>
          <div className="item-actions full">
            <button type="submit" className="submit">Aplicar filtros</button>
            <button type="button" className="submit cancel" onClick={onClearFilters}>Limpar filtros</button>
          </div>
        </form>
      </section>

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Nova anotacao</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Titulo *<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
          <label>Categoria<input name="categoria" value={form.categoria} onChange={onChange} maxLength={50} placeholder="Ex: Manutencao, Financeiro..." /></label>
          <label>
            Importancia
            <select name="importancia" value={form.importancia} onChange={onChange}>
              {IMPORTANCIAS.map((i) => <option key={i} value={i}>{i}</option>)}
            </select>
          </label>
          <label>Referencia<input name="referencia" value={form.referencia} onChange={onChange} maxLength={200} placeholder="Ex: Nr documento, protocolo..." /></label>
          <label className="full">Descricao<textarea name="descricao" value={form.descricao} onChange={onChange} rows={3} /></label>
          <button type="submit" disabled={submitting} className="submit full">
            {submitting ? 'Salvando...' : 'Registrar anotacao'}
          </button>
        </form>
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <p className="muted">Carregando...</p> : null}
        {!loading && items.length === 0 ? <p className="muted">Nenhuma anotacao encontrada para os filtros selecionados.</p> : null}
        {items.map((a) => (
          <article key={a.id} className="item">
            {editing[a.id] ? (
              <>
                <label>Titulo<input name="titulo" value={editing[a.id].titulo} onChange={(e) => onEditChange(a.id, e)} /></label>
                <label>Categoria<input name="categoria" value={editing[a.id].categoria} onChange={(e) => onEditChange(a.id, e)} /></label>
                <label>
                  Importancia
                  <select name="importancia" value={editing[a.id].importancia} onChange={(e) => onEditChange(a.id, e)}>
                    {IMPORTANCIAS.map((i) => <option key={i} value={i}>{i}</option>)}
                  </select>
                </label>
                <label>Referencia<input name="referencia" value={editing[a.id].referencia} onChange={(e) => onEditChange(a.id, e)} /></label>
                <label className="full">Descricao<textarea name="descricao" value={editing[a.id].descricao} onChange={(e) => onEditChange(a.id, e)} rows={3} /></label>
                <div className="item-actions">
                  <button className="submit" style={{ flex: 1 }} onClick={() => onUpdate(a.id)}>Salvar</button>
                  <button className="submit cancel" onClick={() => setEditing((prev) => { const c = { ...prev }; delete c[a.id]; return c })}>Cancelar</button>
                </div>
              </>
            ) : (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <h3 style={{ margin: 0 }}>{a.titulo}</h3>
                  <span className={importanciaClass(a.importancia)}>{a.importancia}</span>
                </div>
                {a.categoria ? <p className="muted" style={{ marginTop: 4 }}>Categoria: {a.categoria}</p> : null}
                {a.descricao ? <p style={{ marginTop: 6 }}>{a.descricao}</p> : null}
                {a.referencia ? <p className="muted" style={{ marginTop: 4 }}>Ref: {a.referencia}</p> : null}
                <div className="item-actions">
                  <button className="submit" onClick={() => startEdit(a)}>Editar</button>
                  <button className="submit danger" onClick={() => onDelete(a.id)}>Excluir</button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>
    </>
  )
}

export default AnotacoesPage
