import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import Button from './components/ui/Button'
import ManutencaoForm from './components/ManutencaoForm'
import { formatCurrency, statusLabel } from './components/manutencaoUtils'

const CURRENCY_FIELDS = ['custoPrevisto', 'custoRealizado']

function ManutencoesPage() {
  const formRef = useRef(null)
  const [editing, setEditing] = useState(null)
  const [items, setItems] = useState([])
  const [prestadores, setPrestadores] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

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

  function getPrestadorById(prestadorId) {
    return prestadores.find((p) => p.id === prestadorId) || null
  }

  function getWhatsAppLink(phone) {
    const digits = (phone || '').replace(/\D/g, '')
    if (!digits) return null
    return `https://wa.me/${digits}`
  }

  function startEdit(manutencao) {
    setError('')
    setSuccess('')
    setEditing(manutencao)
    window.setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  function cancelEdit() {
    setEditing(null)
  }

  async function onSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Manutenção atualizada com sucesso.' : 'Manutenção registrada com sucesso.')
    setEditing(null)
    await load()
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Gestão de manutenção</p>
        <h1>Manutenções</h1>
        <p className="subtitle">Registre manutenções preventivas e corretivas com custos, status e responsável.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel section-spacer" ref={formRef}>
        <h2>{editing ? 'Editar manutenção' : 'Nova manutenção'}</h2>
        <ManutencaoForm
          key={editing?.id ?? 'nova'}
          manutencao={editing}
          onSaved={onSaved}
          onCancel={editing ? cancelEdit : undefined}
        />
      </section>

      <section className="board section-spacer">
        {loading ? <LoadingState message="Carregando manutenções..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhuma manutenção registrada." /> : null}
        {items.map((m) => (
          <article key={m.id} className="item">
            {(() => {
              const prestador = m.fornecedorId ? getPrestadorById(m.fornecedorId) : null
              return (
                <>
                  <h3 className="item-title">{m.titulo}</h3>
                  <p className="muted item-meta">{m.tipo} · {statusLabel(m.status)}{m.categoria ? ` · ${m.categoria}` : ''}</p>
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
                  {m.descricao ? <p className="item-description">{m.descricao}</p> : null}
                  {m.observacoes ? <p className="muted item-description">Obs: {m.observacoes}</p> : null}
                  <div className="item-actions">
                    <Button type="button" onClick={() => startEdit(m)}>
                      Editar
                    </Button>
                  </div>
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
