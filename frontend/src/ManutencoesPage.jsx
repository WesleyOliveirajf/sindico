import { useEffect, useState } from 'react'
import { apiFetch, parseJson } from './api'

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

function ManutencoesPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [items, setItems] = useState([])
  const [prestadores, setPrestadores] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/manutencoes')
      if (!res.ok) throw new Error('Falha ao carregar manutencoes.')
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

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
        custoPrevisto: form.custoPrevisto ? Number(form.custoPrevisto) : null,
        custoRealizado: form.custoRealizado ? Number(form.custoRealizado) : null,
        fornecedorId: form.fornecedorId || null,
      }
      const res = await apiFetch('/api/manutencoes', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || 'Erro ao registrar manutencao.')
      }
      setSuccess('Manutencao registrada com sucesso.')
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
        <p className="eyebrow">Gestao de manutencao</p>
        <h1>Manutencoes</h1>
        <p className="subtitle">Registre manutencoes preventivas e corretivas com custos, status e responsavel.</p>
      </section>

      {error ? <p className="message error">{error}</p> : null}
      {success ? <p className="message success">{success}</p> : null}

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Nova manutencao</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Titulo *<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
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
          <label>Responsavel interno<input name="responsavelInterno" value={form.responsavelInterno} onChange={onChange} maxLength={150} /></label>
          <label>Status<select name="status" value={form.status} onChange={onChange}><option value="ABERTA">Aberta</option><option value="AGENDADA">Agendada</option><option value="EM_ANDAMENTO">Em andamento</option><option value="CONCLUIDA">Concluida</option><option value="CANCELADA">Cancelada</option></select></label>
          <label>Data da ocorrencia<input type="date" name="dataOcorrencia" value={form.dataOcorrencia} onChange={onChange} /></label>
          <label>Data da execucao<input type="date" name="dataExecucao" value={form.dataExecucao} onChange={onChange} /></label>
          <label>Custo previsto<input type="number" step="0.01" name="custoPrevisto" value={form.custoPrevisto} onChange={onChange} /></label>
          <label>Custo realizado<input type="number" step="0.01" name="custoRealizado" value={form.custoRealizado} onChange={onChange} /></label>
          <label className="full">Descricao<textarea name="descricao" value={form.descricao} onChange={onChange} rows={3} /></label>
          <label className="full">Observacoes<textarea name="observacoes" value={form.observacoes} onChange={onChange} rows={2} /></label>
          <button type="submit" disabled={submitting} className="submit full">{submitting ? 'Salvando...' : 'Registrar manutencao'}</button>
        </form>
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <p className="muted">Carregando...</p> : null}
        {!loading && items.length === 0 ? <p className="muted">Nenhuma manutencao registrada.</p> : null}
        {items.map((m) => (
          <article key={m.id} className="item">
            {(() => {
              const prestador = m.fornecedorId ? getPrestadorById(m.fornecedorId) : null
              return (
                <>
                  <h3 style={{ margin: 0 }}>{m.titulo}</h3>
                  <p className="muted" style={{ marginTop: 4 }}>{m.tipo} · {m.status}{m.categoria ? ` · ${m.categoria}` : ''}</p>
                  {m.dataOcorrencia ? <p className="muted">Ocorrencia: {m.dataOcorrencia}</p> : null}
                  {m.dataExecucao ? <p className="muted">Execucao: {m.dataExecucao}</p> : null}
                  {m.local ? <p className="muted">Local: {m.local}</p> : null}
                  {m.fornecedorId ? <p className="muted">Prestador: {prestador?.nome || 'Prestador nao encontrado'}</p> : null}
                  {prestador?.telefone ? (
                    <p className="muted">
                      Telefone do prestador:{' '}
                      <a href={getWhatsAppLink(prestador.telefone)} target="_blank" rel="noreferrer">
                        {prestador.telefone}
                      </a>
                    </p>
                  ) : null}
                  {m.responsavelInterno ? <p className="muted">Responsavel: {m.responsavelInterno}</p> : null}
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
