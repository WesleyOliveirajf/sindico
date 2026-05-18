import { useEffect, useState } from 'react'
import { apiFetch, parseJson } from './api'

const TIPO_LABELS = { MANUTENCAO: 'Manutencao', REUNIAO: 'Reuniao', OUTROS: 'Outros' }

const INITIAL_FORM = {
  titulo: '',
  descricao: '',
  inicioEm: '',
  local: '',
  tipo: 'OUTROS',
}

function CompromissosPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [filtro, setFiltro] = useState('abertos')
  const [form, setForm] = useState(INITIAL_FORM)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/compromissos')
      if (!res.ok) throw new Error('Falha ao carregar compromissos.')
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

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const res = await apiFetch('/api/compromissos', {
        method: 'POST',
        body: JSON.stringify(form),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || 'Erro ao salvar compromisso.')
      }
      setSuccess('Compromisso criado com sucesso.')
      setForm(INITIAL_FORM)
      setShowForm(false)
      setFiltro('abertos')
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function onConcluir(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/compromissos/${id}/concluir`, { method: 'PATCH' })
      if (!res.ok) throw new Error('Erro ao concluir compromisso.')
      setSuccess('Compromisso marcado como concluido.')
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onDelete(id) {
    if (!confirm('Deseja excluir este compromisso?')) return
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/compromissos/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Erro ao excluir compromisso.')
      setSuccess('Compromisso excluido.')
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

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Agenda do condominio</p>
        <h1>Compromissos</h1>
        <p className="subtitle">Gerencie compromissos, tarefas e prazos do condominio em um so lugar.</p>
      </section>

      {error ? <p className="message error" style={{ marginTop: 16 }}>{error}</p> : null}
      {success ? <p className="message success" style={{ marginTop: 16 }}>{success}</p> : null}

      <div style={{ marginTop: 20, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
        <div className="auth-mode-toggle" style={{ maxWidth: 320, marginBottom: 0 }}>
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
            Concluidos{concluidos.length > 0 ? ` (${concluidos.length})` : ''}
          </button>
        </div>
        <button
          className="submit"
          onClick={() => setShowForm((v) => !v)}
          style={{ marginLeft: 'auto' }}
        >
          {showForm ? 'Cancelar' : '+ Novo Compromisso'}
        </button>
      </div>

      {showForm && (
        <section className="panel" style={{ marginTop: 16 }}>
          <h2>Novo Compromisso</h2>
          <form onSubmit={onSubmit} className="form-grid">
            <label className="full">
              Titulo *
              <input
                name="titulo"
                value={form.titulo}
                onChange={onChange}
                required
                maxLength={150}
                placeholder="Ex: Vistoria da bomba d'agua"
              />
            </label>
            <label>
              Data de inicio *
              <input type="date" name="inicioEm" value={form.inicioEm} onChange={onChange} required />
            </label>
            <label>
              Tipo
              <select name="tipo" value={form.tipo} onChange={onChange}>
                <option value="OUTROS">Outros</option>
                <option value="MANUTENCAO">Manutencao</option>
                <option value="REUNIAO">Reuniao</option>
              </select>
            </label>
            <label className="full">
              Local
              <input
                name="local"
                value={form.local}
                onChange={onChange}
                maxLength={150}
                placeholder="Ex: Sala de reunioes, Subsolo..."
              />
            </label>
            <label className="full">
              Descricao
              <textarea
                name="descricao"
                value={form.descricao}
                onChange={onChange}
                rows={2}
                placeholder="Detalhes do compromisso..."
              />
            </label>
            <button type="submit" disabled={submitting} className="submit full">
              {submitting ? 'Salvando...' : 'Criar compromisso'}
            </button>
          </form>
        </section>
      )}

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <p className="muted">Carregando...</p> : null}
        {!loading && lista.length === 0 ? (
          <p className="muted">
            {filtro === 'abertos'
              ? 'Nenhum compromisso em aberto. Clique em "+ Novo Compromisso" para adicionar.'
              : 'Nenhum compromisso concluido ainda.'}
          </p>
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
                    title="Marcar como concluido"
                    onClick={() => onConcluir(c.id)}
                    style={{
                      flexShrink: 0, marginTop: 3, width: 20, height: 20,
                      border: '2px solid var(--border)', borderRadius: 6,
                      background: '#fff', cursor: 'pointer', padding: 0,
                    }}
                    aria-label="Marcar como concluido"
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
                      {concluido ? 'Concluido' : 'Em aberto'}
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
                {c.inicioEm && <div>Inicio: {formatData(c.inicioEm)}</div>}
                {c.fimEm && <div>Concluido em: {formatDataHora(c.fimEm)}</div>}
                {c.local && <div>Local: {c.local}</div>}
              </div>

              <div className="item-actions">
                <button className="submit danger" onClick={() => onDelete(c.id)}>Excluir</button>
              </div>
            </article>
          )
        })}
      </section>
    </>
  )
}

export default CompromissosPage