import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson, iaGerarAta } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ReuniaoForm from './components/ReuniaoForm'

function ReunioesPage() {
  const formRef = useRef(null)
  const [editing, setEditing] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
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

  function startEdit(reuniao) {
    setError('')
    setSuccess('')
    setEditing(reuniao)
    window.setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  async function onSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Reunião atualizada com sucesso.' : 'Reunião registrada com sucesso.')
    setEditing(null)
    await load()
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Gestão de reuniões</p>
        <h1>Reuniões</h1>
        <p className="subtitle">Registre pauta, decisões, participantes e pendências das reuniões do condomínio.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel" style={{ marginTop: 20 }} ref={formRef}>
        <h2>{editing ? 'Editar reunião' : 'Nova reunião'}</h2>
        <ReuniaoForm
          key={editing?.id ?? 'nova'}
          item={editing}
          onSaved={onSaved}
          onCancel={editing ? () => setEditing(null) : undefined}
        />
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

            <div className="item-actions" style={{ marginTop: 10 }}>
              <button className="submit" style={{ fontSize: '0.82rem', padding: '7px 14px' }} onClick={() => startEdit(r)}>
                Editar
              </button>
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
