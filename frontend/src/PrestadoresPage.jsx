import { useEffect, useState } from 'react'
import { apiFetch, parseJson } from './api'

const INITIAL_HISTORY_ITEM = { servico: '', valor: '' }
const INITIAL_FORM = { nome: '', telefone: '', historicoServicos: '', historicoItens: [{ ...INITIAL_HISTORY_ITEM }] }

function mapHistoricoItens(itens) {
  if (!Array.isArray(itens)) return []
  return itens
    .map((item) => ({
      servico: item?.servico || '',
      valor: item?.valor != null ? String(item.valor) : '',
    }))
    .filter((item) => item.servico || item.valor)
}

function normalizePayload(data) {
  const historicoItens = mapHistoricoItens(data.historicoItens)
    .map((item) => ({ servico: item.servico.trim(), valor: Number(item.valor) }))
    .filter((item) => item.servico && Number.isFinite(item.valor) && item.valor > 0)

  return {
    nome: data.nome,
    telefone: data.telefone,
    historicoServicos: data.historicoServicos,
    historicoItens,
  }
}

function hasValidHistoryItems(data) {
  return normalizePayload(data).historicoItens.length > 0
}

function formatCurrency(value) {
  if (value == null || Number.isNaN(Number(value))) return '-'
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value))
}

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
      const res = await apiFetch('/api/prestadores')
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

  function onHistoryChange(setter, idOrNull, index, field, value) {
    setter((prev) => {
      const source = idOrNull ? (prev[idOrNull] || {}) : prev
      const historicoItens = [...(source.historicoItens || [{ ...INITIAL_HISTORY_ITEM }])]
      historicoItens[index] = { ...(historicoItens[index] || INITIAL_HISTORY_ITEM), [field]: value }

      if (idOrNull) {
        return {
          ...prev,
          [idOrNull]: {
            ...source,
            historicoItens,
          },
        }
      }

      return { ...prev, historicoItens }
    })
  }

  function addHistoryItem(setter, idOrNull) {
    setter((prev) => {
      const source = idOrNull ? (prev[idOrNull] || {}) : prev
      const historicoItens = [...(source.historicoItens || [{ ...INITIAL_HISTORY_ITEM }]), { ...INITIAL_HISTORY_ITEM }]

      if (idOrNull) {
        return { ...prev, [idOrNull]: { ...source, historicoItens } }
      }

      return { ...prev, historicoItens }
    })
  }

  function removeHistoryItem(setter, idOrNull, index) {
    setter((prev) => {
      const source = idOrNull ? (prev[idOrNull] || {}) : prev
      const current = source.historicoItens || [{ ...INITIAL_HISTORY_ITEM }]
      const historicoItens = current.filter((_, idx) => idx !== index)
      const finalItems = historicoItens.length > 0 ? historicoItens : [{ ...INITIAL_HISTORY_ITEM }]

      if (idOrNull) {
        return { ...prev, [idOrNull]: { ...source, historicoItens: finalItems } }
      }

      return { ...prev, historicoItens: finalItems }
    })
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
        historicoServicos: p.historicoServicos || '',
        historicoItens: mapHistoricoItens(p.historicoItens),
      },
    }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    if (!hasValidHistoryItems(form)) {
      setError('Adicione ao menos um servico com valor no historico.')
      return
    }
    setSubmitting(true)
    try {
      const res = await apiFetch('/api/prestadores', {
        method: 'POST',
        body: JSON.stringify(normalizePayload(form)),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || 'Erro ao cadastrar prestador.')
      }
      setSuccess('Prestador cadastrado com sucesso.')
      setForm({ ...INITIAL_FORM, historicoItens: [{ ...INITIAL_HISTORY_ITEM }] })
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
    if (!hasValidHistoryItems(data)) {
      setError('Adicione ao menos um servico com valor no historico antes de salvar.')
      return
    }
    try {
      const res = await apiFetch(`/api/prestadores/${id}`, {
        method: 'PUT',
        body: JSON.stringify(normalizePayload(data)),
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
      const res = await apiFetch(`/api/prestadores/${id}/inativar`, { method: 'POST' })
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
          <div className="full">
            <p className="history-title">Historico de servicos e valores</p>
            <div className="history-list">
              {form.historicoItens.map((item, index) => (
                <div className="history-row" key={`new-history-${index}`}>
                  <input
                    name={`servico-${index}`}
                    value={item.servico}
                    onChange={(e) => onHistoryChange(setForm, null, index, 'servico', e.target.value)}
                    maxLength={200}
                    placeholder="Servico realizado"
                  />
                  <input
                    name={`valor-${index}`}
                    value={item.valor}
                    onChange={(e) => onHistoryChange(setForm, null, index, 'valor', e.target.value)}
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Valor"
                  />
                  <button type="button" className="submit cancel" onClick={() => removeHistoryItem(setForm, null, index)}>
                    Remover
                  </button>
                </div>
              ))}
            </div>
            <button type="button" className="submit cancel" onClick={() => addHistoryItem(setForm, null)}>
              Adicionar servico
            </button>
          </div>
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
                <div>
                  <p className="history-title">Historico de servicos e valores</p>
                  <div className="history-list">
                    {(editing[p.id].historicoItens || [{ ...INITIAL_HISTORY_ITEM }]).map((item, index) => (
                      <div className="history-row" key={`${p.id}-history-${index}`}>
                        <input
                          value={item.servico}
                          onChange={(e) => onHistoryChange(setEditing, p.id, index, 'servico', e.target.value)}
                          maxLength={200}
                          placeholder="Servico realizado"
                        />
                        <input
                          value={item.valor}
                          onChange={(e) => onHistoryChange(setEditing, p.id, index, 'valor', e.target.value)}
                          type="number"
                          min="0"
                          step="0.01"
                          placeholder="Valor"
                        />
                        <button type="button" className="submit cancel" onClick={() => removeHistoryItem(setEditing, p.id, index)}>
                          Remover
                        </button>
                      </div>
                    ))}
                  </div>
                  <button type="button" className="submit cancel" onClick={() => addHistoryItem(setEditing, p.id)}>
                    Adicionar servico
                  </button>
                </div>
                <div className="item-actions">
                  <button className="submit" style={{ flex: 1 }} onClick={() => onUpdate(p.id)}>Salvar</button>
                  <button className="submit cancel" onClick={() => setEditing((prev) => { const c = { ...prev }; delete c[p.id]; return c })}>Cancelar</button>
                </div>
              </>
            ) : (
              <>
                <h3>{p.nome}</h3>
                <p className="phone">{p.telefone}</p>
                {Array.isArray(p.historicoItens) && p.historicoItens.length > 0 ? (
                  <div className="history-table">
                    {p.historicoItens.map((item, index) => (
                      <p key={`${p.id}-view-${index}`} className="history-line">
                        <span>{item.servico}</span>
                        <strong>{formatCurrency(item.valor)}</strong>
                      </p>
                    ))}
                  </div>
                ) : p.historicoServicos ? (
                  <p className="history">{p.historicoServicos}</p>
                ) : (
                  <p className="muted">Sem historico registrado.</p>
                )}
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
