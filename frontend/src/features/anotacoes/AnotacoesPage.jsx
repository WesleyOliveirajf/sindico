import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'

const IMPORTANCIAS = ['NORMAL', 'IMPORTANTE', 'CRITICO']

const INITIAL_FORM = { titulo: '', categoria: '', descricao: '', referencia: '', importancia: 'NORMAL', dataReferencia: '' }
const INITIAL_FILTERS = { texto: '', dataInicio: '', dataFim: '' }

/** Valor para input[type=date] a partir do JSON da API. */
function formatDateIso(value) {
  if (value == null) return ''
  if (typeof value === 'string') return value.length >= 10 ? value.slice(0, 10) : value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, m, d] = value
    return `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return ''
}

function AnotacoesPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [editing, setEditing] = useState({})
  const [items, setItems] = useState([])
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingDeleteId, setPendingDeleteId] = useState(null)

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
      if (!res.ok) {
        throw new Error(await parseError(res, 'Falha ao carregar anotações.'))
      }
      setItems(await parseJson(res))
    } catch (err) {
      setSuccess('')
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
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

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
        dataReferencia: formatDateIso(a.dataReferencia),
      },
    }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const payload = {
        titulo: form.titulo,
        categoria: form.categoria || null,
        descricao: form.descricao || null,
        referencia: form.referencia || null,
        importancia: form.importancia,
        dataReferencia: form.dataReferencia || null,
      }
      const res = await apiFetch('/api/anotacoes', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao registrar anotação.'))
      }
      setSuccess('Anotação registrada com sucesso.')
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
        body: JSON.stringify({
          titulo: data.titulo,
          categoria: data.categoria || null,
          descricao: data.descricao || null,
          referencia: data.referencia || null,
          importancia: data.importancia,
          dataReferencia: data.dataReferencia || null,
        }),
      })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao atualizar anotação.'))
      setSuccess('Anotação atualizada com sucesso.')
      setEditing((prev) => { const c = { ...prev }; delete c[id]; return c })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onDelete(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/anotacoes/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao excluir anotação.'))
      setSuccess('Anotação excluída com sucesso.')
      setPendingDeleteId(null)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  const importanciaClass = (i) => i === 'CRITICO' ? 'badge badge--critico' : i === 'IMPORTANTE' ? 'badge badge--importante' : 'badge'

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Registro de ocorrências</p>
        <h1>Anotações</h1>
        <p className="subtitle">Registre informações, observações e ocorrências relevantes do condomínio.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Filtros de busca</h2>
        <form onSubmit={onApplyFilters} className="form-grid">
          <label className="full">
            Buscar por título, categoria, descrição ou referência
            <input
              name="texto"
              value={filters.texto}
              onChange={onFilterChange}
              placeholder="Ex: reunião, vazamento, orçamento"
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
        <h2>Nova anotação</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Título *<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
          <label>Categoria<input name="categoria" value={form.categoria} onChange={onChange} maxLength={50} placeholder="Ex: Manutenção, Financeiro..." /></label>
          <label>
            Data da ocorrência (opcional)
            <input type="date" name="dataReferencia" value={form.dataReferencia} onChange={onChange} />
          </label>
          <label>
            Importância
            <select name="importancia" value={form.importancia} onChange={onChange}>
              {IMPORTANCIAS.map((i) => <option key={i} value={i}>{i}</option>)}
            </select>
          </label>
          <label>Referência<input name="referencia" value={form.referencia} onChange={onChange} maxLength={200} placeholder="Ex: nº documento, protocolo..." /></label>
          <label className="full">Descrição<textarea name="descricao" value={form.descricao} onChange={onChange} rows={3} /></label>
          <button type="submit" disabled={submitting} className="submit full">
            {submitting ? 'Salvando...' : 'Registrar anotação'}
          </button>
        </form>
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <LoadingState message="Carregando anotações..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhuma anotação encontrada para os filtros selecionados." /> : null}
        {items.map((a) => (
          <article key={a.id} className="item">
            {editing[a.id] ? (
              <>
                <label>Título<input name="titulo" value={editing[a.id].titulo} onChange={(e) => onEditChange(a.id, e)} /></label>
                <label>Categoria<input name="categoria" value={editing[a.id].categoria} onChange={(e) => onEditChange(a.id, e)} /></label>
                <label>
                  Importância
                  <select name="importancia" value={editing[a.id].importancia} onChange={(e) => onEditChange(a.id, e)}>
                    {IMPORTANCIAS.map((i) => <option key={i} value={i}>{i}</option>)}
                  </select>
                </label>
                <label>Referência<input name="referencia" value={editing[a.id].referencia} onChange={(e) => onEditChange(a.id, e)} /></label>
                <label>
                  Data da ocorrência (opcional)
                  <input type="date" name="dataReferencia" value={editing[a.id].dataReferencia ?? ''} onChange={(e) => onEditChange(a.id, e)} />
                </label>
                <label className="full">Descrição<textarea name="descricao" value={editing[a.id].descricao} onChange={(e) => onEditChange(a.id, e)} rows={3} /></label>
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
                {formatDateIso(a.dataReferencia) ? (
                  <p className="muted" style={{ marginTop: 4 }}>
                    Data da ocorrência: {new Date(`${formatDateIso(a.dataReferencia)}T12:00:00`).toLocaleDateString('pt-BR')}
                  </p>
                ) : null}
                {a.descricao ? <p style={{ marginTop: 6 }}>{a.descricao}</p> : null}
                {a.referencia ? <p className="muted" style={{ marginTop: 4 }}>Ref: {a.referencia}</p> : null}
                <div className="item-actions">
                  <button className="submit" onClick={() => startEdit(a)}>Editar</button>
                  <button className="submit danger" onClick={() => setPendingDeleteId(a.id)}>Excluir</button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>

      <ConfirmDialog
        open={pendingDeleteId != null}
        title="Excluir anotação"
        message="Deseja excluir esta anotação? Esta ação não pode ser desfeita."
        confirmLabel="Excluir"
        onCancel={() => setPendingDeleteId(null)}
        onConfirm={() => onDelete(pendingDeleteId)}
      />
    </>
  )
}

export default AnotacoesPage
