import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson, iaGerarAta } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'

const INITIAL_FORM = {
  titulo: '',
  tipo: 'ORDINARIA',
  dataHora: '',
  local: '',
  link: '',
  pauta: '',
  resumo: '',
  decisoes: '',
  pendenciasGeradas: '',
  participantesTexto: '',
}

function ReunioesPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [ataLoading, setAtaLoading] = useState(null)
  const [atas, setAtas] = useState({})

  async function load() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/reunioes')
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar reuniões.'))
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

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const participantes = form.participantesTexto
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean)
        .map((nome) => ({ nome, presente: true }))

      const payload = {
        ...form,
        dataHora: form.dataHora || null,
        participantes,
      }
      delete payload.participantesTexto

      const res = await apiFetch('/api/reunioes', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao registrar reunião.'))
      }
      setSuccess('Reunião registrada com sucesso.')
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
        <p className="eyebrow">Gestão de reuniões</p>
        <h1>Reuniões</h1>
        <p className="subtitle">Registre pauta, decisões, participantes e pendências das reuniões do condomínio.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Nova reunião</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>Título *<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
          <label>Tipo<select name="tipo" value={form.tipo} onChange={onChange}><option value="ORDINARIA">Ordinária</option><option value="EXTRAORDINARIA">Extraordinária</option><option value="CONSELHO">Conselho</option><option value="ASSEMBLEIA">Assembleia</option></select></label>
          <label>Data e horário *<input type="datetime-local" name="dataHora" value={form.dataHora} onChange={onChange} required /></label>
          <label>Local<input name="local" value={form.local} onChange={onChange} maxLength={150} /></label>
          <label className="full">Link<input name="link" value={form.link} onChange={onChange} maxLength={500} /></label>
          <label className="full">Pauta<textarea name="pauta" value={form.pauta} onChange={onChange} rows={2} /></label>
          <label className="full">Resumo<textarea name="resumo" value={form.resumo} onChange={onChange} rows={2} /></label>
          <label className="full">Decisões<textarea name="decisoes" value={form.decisoes} onChange={onChange} rows={2} /></label>
          <label className="full">Pendências geradas<textarea name="pendenciasGeradas" value={form.pendenciasGeradas} onChange={onChange} rows={2} /></label>
          <label className="full">Participantes (1 por linha)<textarea name="participantesTexto" value={form.participantesTexto} onChange={onChange} rows={3} placeholder="Maria Silva\nJoão Souza" /></label>
          <button type="submit" disabled={submitting} className="submit full">{submitting ? 'Salvando...' : 'Registrar reunião'}</button>
        </form>
      </section>

      <section className="board" style={{ marginTop: 20 }}>
        {loading ? <LoadingState message="Carregando reuniões..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhuma reunião registrada." /> : null}
        {items.map((r) => (
          <article key={r.id} className="item">
            <h3 style={{ margin: 0 }}>{r.titulo}</h3>
            <p className="muted" style={{ marginTop: 4 }}>{r.tipo} · {new Date(r.dataHora).toLocaleString('pt-BR')}</p>
            {r.local ? <p className="muted">Local: {r.local}</p> : null}
            {r.pauta ? <p style={{ marginTop: 6 }}><strong>Pauta:</strong> {r.pauta}</p> : null}
            {r.decisoes ? <p style={{ marginTop: 6 }}><strong>Decisões:</strong> {r.decisoes}</p> : null}
            {r.pendenciasGeradas ? <p style={{ marginTop: 6 }}><strong>Pendências:</strong> {r.pendenciasGeradas}</p> : null}
            {r.participantes?.length ? <p className="muted">Participantes: {r.participantes.map((p) => p.nome).join(', ')}</p> : null}

            <div style={{ marginTop: 10 }}>
              <button
                className="submit"
                style={{ fontSize: '0.82rem', padding: '7px 14px', background: '#6d28d9' }}
                disabled={ataLoading === r.id}
                onClick={async () => {
                  setAtaLoading(r.id)
                  try {
                    const data = await iaGerarAta(r.id)
                    setAtas((prev) => ({ ...prev, [r.id]: data.ata }))
                  } catch (err) {
                    setAtas((prev) => ({ ...prev, [r.id]: `Erro: ${err.message}` }))
                  } finally {
                    setAtaLoading(null)
                  }
                }}
              >
                {ataLoading === r.id ? 'Gerando ata...' : 'Gerar Ata com IA'}
              </button>
            </div>

            {atas[r.id] && (
              <div className="ia-result-box" style={{ marginTop: 10 }}>
                <p style={{ margin: '0 0 6px', fontWeight: 700, fontSize: '0.88rem' }}>Ata gerada:</p>
                <pre className="ia-result-text">{atas[r.id]}</pre>
              </div>
            )}
          </article>
        ))}
      </section>
    </>
  )
}

export default ReunioesPage
