import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import PrestadorForm from './components/PrestadorForm'
import Button from './components/ui/Button'

function PrestadoresPage() {
  const formRef = useRef(null)
  const [editing, setEditing] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [pendingInactivateId, setPendingInactivateId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadPrestadores() {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/prestadores')
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar prestadores.'))
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadPrestadores()
    }, 0)
    return () => clearTimeout(timer)
  }, [])

  function startEdit(prestador) {
    setError('')
    setSuccess('')
    setEditing(prestador)
    window.setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  async function onSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Prestador atualizado com sucesso.' : 'Prestador cadastrado com sucesso.')
    setEditing(null)
    await loadPrestadores()
  }

  async function onInactivate(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/prestadores/${id}/inativar`, { method: 'POST' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao inativar prestador.'))
      setSuccess('Prestador inativado com sucesso.')
      setPendingInactivateId(null)
      if (editing?.id === id) setEditing(null)
      await loadPrestadores()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Serviços terceirizados</p>
        <h1>Prestadores de serviço</h1>
        <p className="subtitle">Cadastre nome, telefone e área de atuação dos profissionais.</p>
      </section>

      <SuccessState message={success} />

      <section className="panel section-spacer" ref={formRef}>
        <h2>{editing ? 'Editar prestador' : 'Novo prestador'}</h2>
        <PrestadorForm
          key={editing?.id ?? 'novo'}
          item={editing}
          onSaved={onSaved}
          onCancel={editing ? () => setEditing(null) : undefined}
        />
      </section>

      <section className="board section-spacer">
        {loading ? <LoadingState message="Carregando prestadores..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={loadPrestadores} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhum prestador cadastrado." /> : null}
        {items.map((p) => (
          <article key={p.id} className="item prestador-item">
            <h3>{p.nome}</h3>
            <p className="phone">{p.telefone}</p>
            {p.historicoServicos ? <p className="history">Área de atuação: {p.historicoServicos}</p> : <p className="muted">Área de atuação não informada.</p>}
            <div className="item-actions">
              <Button onClick={() => startEdit(p)}>Editar</Button>
              <Button variant="danger" onClick={() => setPendingInactivateId(p.id)}>Inativar</Button>
            </div>
          </article>
        ))}
      </section>

      <ConfirmDialog
        open={pendingInactivateId != null}
        title="Inativar prestador"
        message="Deseja inativar este prestador? Ele deixará de aparecer na lista de prestadores ativos."
        confirmLabel="Inativar"
        onCancel={() => setPendingInactivateId(null)}
        onConfirm={() => onInactivate(pendingInactivateId)}
      />
    </>
  )
}

export default PrestadoresPage
