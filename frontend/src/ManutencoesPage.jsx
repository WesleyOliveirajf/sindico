import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson, iaTriarManutencao } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'

const INITIAL_FORM = {
  titulo: '',
  descricao: '',
  tipo: 'PREVENTIVA',
  categoria: '',
  local: '',
  fornecedorId: '',
  responsavelInterno: '',
  dataOcorrencia: '',
  dataExecucao: '',
  custoPrevisto: '',
  custoRealizado: '',
  status: 'ABERTA',
  observacoes: '',
}

const CURRENCY_FIELDS = new Set(['custoPrevisto', 'custoRealizado'])

function parseCurrencyValue(value) {
  if (value == null || value === '') return null
  if (typeof value === 'number') return Number.isFinite(value) ? value : null

  const sanitized = String(value).trim().replace(/[^\d,.-]/g, '')
  if (!/\d/.test(sanitized)) return null

  const lastComma = sanitized.lastIndexOf(',')
  const lastDot = sanitized.lastIndexOf('.')
  let normalized = sanitized

  if (lastComma > -1 && lastDot > -1) {
    normalized = lastComma > lastDot
      ? sanitized.replace(/\./g, '').replace(',', '.')
      : sanitized.replace(/,/g, '')
  } else if (lastComma > -1) {
    normalized = sanitized.replace(/\./g, '').replace(',', '.')
  } else if (lastDot > -1) {
    const dotCount = (sanitized.match(/\./g) || []).length
    const decimalDigits = sanitized.length - lastDot - 1
    normalized = dotCount === 1 && decimalDigits <= 2
      ? sanitized.replace(/,/g, '')
      : sanitized.replace(/\./g, '')
  }

  const parsed = Number(normalized)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null
}

function formatCurrency(value) {
  const parsed = parseCurrencyValue(value)
  if (parsed == null) return ''
  return parsed.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function ManutencoesPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [items, setItems] = useState([])
  const [prestadores, setPrestadores] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // IA triagem
  const [triando, setTriando] = useState(false)
  const [triagemMsg, setTriagemMsg] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/manutencoes')
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar manutenções.'))
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

  useEffect(() => {
    async function loadPrestadores() {
      try {
        const res = await apiFetch('/api/prestadores')
        if (!res.ok) return
        setPrestadores(await parseJson(res))
      } catch {
        setPrestadores([])
      }
    }
    loadPrestadores()
  }, [])

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function formatCurrencyField(fieldName) {
    setForm((prev) => ({ ...prev, [fieldName]: formatCurrency(prev[fieldName]) }))
  }

  function onCurrencyBlur(e) {
    formatCurrencyField(e.target.name)
  }

  function onCurrencyKeyDown(e) {
    if (e.key !== 'Enter') return
    e.preventDefault()
    formatCurrencyField(e.target.name)
  }

  function getPrestadorById(prestadorId) {
    return prestadores.find((p) => p.id === prestadorId) || null
  }

  function getWhatsAppLink(phone) {
    const digits = (phone || '').replace(/\D/g, '')
    if (!digits) return null
    return `https://wa.me/${digits}`
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const payload = {
        ...form,
        dataOcorrencia: form.dataOcorrencia || null,
        dataExecucao: form.dataExecucao || null,
        custoPrevisto: parseCurrencyValue(form.custoPrevisto),
        custoRealizado: parseCurrencyValue(form.custoRealizado),
        fornecedorId: form.fornecedorId || null,
      }
      const res = await apiFetch('/api/manutencoes', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao registrar manutenção.'))
      }
      setSuccess('Manutenção registrada com sucesso.')
      setForm(INITIAL_FORM)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Gestão de manutenção</p>
        <h1>Manutenções</h1>
        <p className="subtitle">Registre manutenções preventivas e corretivas com custos, status e responsável.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Nova manutenção</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Título *<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
          <label>Tipo<select name="tipo" value={form.tipo} onChange={onChange}><option value="PREVENTIVA">Preventiva</option><option value="CORRETIVA">Corretiva</option></select></label>
          <label>Categoria<input name="categoria" value={form.categoria} onChange={onChange} maxLength={50} /></label>
          <label>Local<input name="local" value={form.local} onChange={onChange} maxLength={150} /></label>
          <label>
            Prestador que realizou *
            <select name="fornecedorId" value={form.fornecedorId} onChange={onChange} required>
              <option value="">Selecione</option>
              {prestadores.map((p) => (
                <option key={p.id} value={p.id}>{p.nome}</option>
              ))}
            </select>
          </label>
          <label>Responsável interno<input name="responsavelInterno" value={form.responsavelInterno} onChange={onChange} maxLength={150} /></label>
          <label>Status<select name="status" value={form.status} onChange={onChange}><option value="ABERTA">Aberta</option><option value="AGENDADA">Agendada</option><option value="EM_ANDAMENTO">Em andamento</option><option value="CONCLUIDA">Concluída</option><option value="CANCELADA">Cancelada</option></select></label>
          <label>Data da ocorrência<input type="date" name="dataOcorrencia" value={form.dataOcorrencia} onChange={onChange} /></label>
          <label>Data da execução<input type="date" name="dataExecucao" value={form.dataExecucao} onChange={onChange} /></label>
          <label>
            Custo previsto
            <input
              type="text"
              inputMode="decimal"
              name="custoPrevisto"
              value={form.custoPrevisto}
              onChange={onChange}
              onBlur={onCurrencyBlur}
              onKeyDown={onCurrencyKeyDown}
              placeholder="R$ 0,00"
            />
          </label>
          <label>
            Custo realizado
            <input
              type="text"
              inputMode="decimal"
              name="custoRealizado"
              value={form.custoRealizado}
              onChange={onChange}
              onBlur={onCurrencyBlur}
              onKeyDown={onCurrencyKeyDown}
              placeholder="R$ 0,00"
            />
          </label>
          <label className="full">Descrição<textarea name="descricao" value={form.descricao} onChange={onChange} rows={3} placeholder="Descreva o problema ou serviço necessário..." /></label>

          <div className="full" style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
            <button
              type="button"
              className="submit"
              style={{ fontSize: '0.82rem', padding: '7px 14px', background: '#6d28d9' }}
              disabled={triando || !form.descricao.trim()}
              onClick={async () => {
                setTriando(true)
                setTriagemMsg('')
                try {
                  const data = await iaTriarManutencao(form.descricao)
                  setForm((prev) => ({
                    ...prev,
                    tipo: data.tipo || prev.tipo,
                    categoria: data.categoria || prev.categoria,
                    titulo: data.tituloSugerido || prev.titulo,
                    observacoes: data.observacoes || prev.observacoes,
                  }))
                  const urgLabel = data.urgencia ? ` · Urgência: ${data.urgencia}` : ''
                  setTriagemMsg(`Triagem concluída: ${data.tipo} · ${data.categoria}${urgLabel}`)
                } catch (err) {
                  setTriagemMsg(`Erro: ${err.message}`)
                } finally {
                  setTriando(false)
                }
              }}
            >
              {triando ? 'Triando...' : 'Triar com IA'}
            </button>
            {triagemMsg && <span className="muted" style={{ fontSize: '0.82rem' }}>{triagemMsg}</span>}
          </div>

          <label className="full">Observações<textarea name="observacoes" value={form.observacoes} onChange={onChange} rows={2} /></label>
          <button type="submit" disabled={submitting} className="submit full">{submitting ? 'Salvando...' : 'Registrar manutenção'}</button>
        </form>
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <LoadingState message="Carregando manutenções..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhuma manutenção registrada." /> : null}
        {items.map((m) => (
          <article key={m.id} className="item">
            {(() => {
              const prestador = m.fornecedorId ? getPrestadorById(m.fornecedorId) : null
              return (
                <>
                  <h3 style={{ margin: 0 }}>{m.titulo}</h3>
                  <p className="muted" style={{ marginTop: 4 }}>{m.tipo} · {m.status}{m.categoria ? ` · ${m.categoria}` : ''}</p>
                  {m.dataOcorrencia ? <p className="muted">Ocorrência: {m.dataOcorrencia}</p> : null}
                  {m.dataExecucao ? <p className="muted">Execução: {m.dataExecucao}</p> : null}
                  {m.local ? <p className="muted">Local: {m.local}</p> : null}
                  {m.fornecedorId ? <p className="muted">Prestador: {prestador?.nome || 'Prestador não encontrado'}</p> : null}
                  {prestador?.telefone ? (
                    <p className="muted">
                      Telefone do prestador:{' '}
                      <a href={getWhatsAppLink(prestador.telefone)} target="_blank" rel="noreferrer">
                        {prestador.telefone}
                      </a>
                    </p>
                  ) : null}
                  {m.responsavelInterno ? <p className="muted">Responsável: {m.responsavelInterno}</p> : null}
                  {CURRENCY_FIELDS.some((field) => m[field] != null) ? (
                    <p className="muted">
                      Custos:{' '}
                      {m.custoPrevisto != null ? `Previsto ${formatCurrency(m.custoPrevisto)}` : 'Previsto -'}
                      {' · '}
                      {m.custoRealizado != null ? `Realizado ${formatCurrency(m.custoRealizado)}` : 'Realizado -'}
                    </p>
                  ) : null}
                  {m.descricao ? <p style={{ marginTop: 6 }}>{m.descricao}</p> : null}
                  {m.observacoes ? <p className="muted" style={{ marginTop: 4 }}>Obs: {m.observacoes}</p> : null}
                </>
              )
            })()}
          </article>
        ))}
      </section>
    </>
  )
}

export default ManutencoesPage
