import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import AnotacaoForm from './components/AnotacaoForm'
import { formatDateIso } from './components/formUtils'

const INITIAL_FILTERS = { texto: '', dataInicio: '', dataFim: '' }

function AnotacoesPage() {
  const formRef = useRef(null)
  const [editing, setEditing] = useState(null)
  const [items, setItems] = useState([])
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [loading, setLoading] = useState(true)
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

  function startEdit(anotacao) {
    setError('')
    setSuccess('')
    setEditing(anotacao)
    window.setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  async function onSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Anotação atualizada com sucesso.' : 'Anotação registrada com sucesso.')
    setEditing(null)
    await load()
  }

  async function onDelete(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/anotacoes/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao excluir anotação.'))
      setSuccess('Anotação excluída com sucesso.')
      setPendingDeleteId(null)
      if (editing?.id === id) setEditing(null)
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

      <section className="panel" style={{ marginTop: 20 }} ref={formRef}>
        <h2>{editing ? 'Editar anotação' : 'Nova anotação'}</h2>
        <AnotacaoForm
          key={editing?.id ?? 'nova'}
          item={editing}
          onSaved={onSaved}
          onCancel={editing ? () => setEditing(null) : undefined}
        />
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <LoadingState message="Carregando anotações..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhuma anotação encontrada para os filtros selecionados." /> : null}
        {items.map((a) => (
          <article key={a.id} className="item">
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
