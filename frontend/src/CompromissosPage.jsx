import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import Button from './components/ui/Button'
import Card from './components/ui/Card'
import CompromissoForm from './components/CompromissoForm'

const TIPO_LABELS = { MANUTENCAO: 'Manutenção', REUNIAO: 'Reunião', OUTROS: 'Outros' }

function CompromissosPage() {
  const formRef = useRef(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  // null = formulário fechado; { item: null } = novo; { item } = edição
  const [formState, setFormState] = useState(null)
  const [filtro, setFiltro] = useState('abertos')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingDeleteId, setPendingDeleteId] = useState(null)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/compromissos')
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar lembretes.'))
      setItems(await parseJson(res))
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

  function openCreateForm() {
    setError('')
    setSuccess('')
    setFormState({ item: null })
  }

  function closeForm() {
    setFormState(null)
  }

  function startEdit(item) {
    setError('')
    setSuccess('')
    setFormState({ item })
    window.setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  async function onSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Lembrete atualizado com sucesso.' : 'Lembrete criado com sucesso.')
    setFormState(null)
    if (!wasEdit) setFiltro('abertos')
    await load()
  }

  async function onConcluir(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/compromissos/${id}/concluir`, { method: 'PATCH' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao concluir lembrete.'))
      setSuccess('Lembrete marcado como concluído.')
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onDelete(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/compromissos/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao excluir lembrete.'))
      setSuccess('Lembrete excluído.')
      setPendingDeleteId(null)
      if (formState?.item?.id === id) setFormState(null)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  const isConcluido = (c) => c.status === 'CONCLUIDO' || c.concluido === true

  const abertos = items.filter((c) => !isConcluido(c))
  const concluidos = items.filter((c) => isConcluido(c))
  const lista = filtro === 'abertos' ? abertos : concluidos

  function formatData(dt) {
    if (!dt) return ''
    return new Date(dt).toLocaleDateString('pt-BR')
  }

  function formatDataHora(dt) {
    if (!dt) return ''
    return new Date(dt).toLocaleString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    })
  }

  const editingId = formState?.item?.id ?? null

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Agenda do condomínio</p>
        <h1>Lembretes</h1>
        <p className="subtitle">Gerencie lembretes, tarefas e prazos do condomínio em um só lugar.</p>
      </section>

      <SuccessState message={success} />

      <div className="page-actions">
        <div className="auth-mode-toggle">
          <button
            className={`mode-button${filtro === 'abertos' ? ' mode-button--active' : ''}`}
            onClick={() => setFiltro('abertos')}
          >
            A Fazer{abertos.length > 0 ? ` (${abertos.length})` : ''}
          </button>
          <button
            className={`mode-button${filtro === 'concluidos' ? ' mode-button--active' : ''}`}
            onClick={() => setFiltro('concluidos')}
          >
            Concluídos{concluidos.length > 0 ? ` (${concluidos.length})` : ''}
          </button>
        </div>
        <Button
          onClick={() => (formState && !editingId ? closeForm() : openCreateForm())}
          className="page-actions__primary"
        >
          {formState && !editingId ? 'Cancelar' : '+ Novo Lembrete'}
        </Button>
      </div>

      {formState && (
        <Card style={{ marginTop: 16 }} ref={formRef}>
          <h2>{editingId ? 'Editar Lembrete' : 'Novo Lembrete'}</h2>
          <CompromissoForm
            key={editingId ?? 'novo'}
            item={formState.item}
            onSaved={onSaved}
            onCancel={closeForm}
          />
        </Card>
      )}

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <LoadingState message="Carregando lembretes..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && lista.length === 0 ? (
          <EmptyState
            message={
              filtro === 'abertos'
                ? 'Nenhum lembrete em aberto. Clique em "+ Novo Lembrete" para adicionar.'
                : 'Nenhum lembrete concluído ainda.'
            }
          />
        ) : null}
        {lista.map((c) => {
          const concluido = isConcluido(c)
          return (
            <article
              key={c.id}
              className="item"
              style={{ borderLeft: `4px solid ${concluido ? '#4ade80' : '#facc15'}` }}
            >
              <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start', marginBottom: 8 }}>
                {!concluido ? (
                  <button
                    title="Marcar como concluído"
                    onClick={() => onConcluir(c.id)}
                    style={{
                      flexShrink: 0, marginTop: 3, width: 20, height: 20,
                      border: '2px solid var(--border)', borderRadius: 6,
                      background: '#fff', cursor: 'pointer', padding: 0,
                    }}
                    aria-label="Marcar como concluído"
                  />
                ) : (
                  <span style={{
                    flexShrink: 0, marginTop: 3, width: 20, height: 20, borderRadius: 6,
                    background: '#4ade80', display: 'flex', alignItems: 'center',
                    justifyContent: 'center', fontSize: 11, color: '#fff', fontWeight: 700,
                  }}>✓</span>
                )}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <h3 style={{
                    margin: 0, fontSize: '1rem',
                    textDecoration: concluido ? 'line-through' : 'none',
                    color: concluido ? 'var(--muted)' : 'var(--ink)',
                  }}>
                    {c.titulo}
                  </h3>
                  <div style={{ marginTop: 6, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    <span className="tag" style={concluido
                      ? {}
                      : { background: '#fef9c3', color: '#854d0e' }}>
                      {concluido ? 'Concluído' : 'Em aberto'}
                    </span>
                    {c.tipo && c.tipo !== 'OUTROS' && (
                      <span className="tag" style={{ background: '#e0f2fe', color: '#0369a1' }}>
                        {TIPO_LABELS[c.tipo] || c.tipo}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {c.descricao ? <p style={{ margin: '0 0 8px' }}>{c.descricao}</p> : null}

              <div className="muted" style={{ fontSize: '0.85rem', lineHeight: 1.6 }}>
                {c.inicioEm && <div>Início: {formatData(c.inicioEm)}</div>}
                {c.fimEm && <div>Concluído em: {formatDataHora(c.fimEm)}</div>}
                {c.local && <div>Local: {c.local}</div>}
              </div>

              <div className="item-actions">
                <Button variant="secondary" onClick={() => startEdit(c)}>Editar</Button>
                <Button variant="danger" onClick={() => setPendingDeleteId(c.id)}>Excluir</Button>
              </div>
              </article>
            )
        })}
      </section>

      <ConfirmDialog
        open={pendingDeleteId != null}
        title="Excluir lembrete"
        message="Deseja excluir este lembrete? Esta ação não pode ser desfeita."
        confirmLabel="Excluir"
        onCancel={() => setPendingDeleteId(null)}
        onConfirm={() => onDelete(pendingDeleteId)}
      />
    </>
  )
}

export default CompromissosPage
