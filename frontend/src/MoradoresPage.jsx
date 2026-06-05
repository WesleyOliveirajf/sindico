import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import Button from './components/ui/Button'
import Input, { Select, Textarea } from './components/ui/Input'

const PAPEIS = ['PROPRIETARIO', 'INQUILINO', 'DEPENDENTE', 'ZELADOR', 'OUTRO']

const INITIAL_UNIDADE = { bloco: '', numero: '', complemento: '' }
const INITIAL_MORADOR = { unidadeId: '', nome: '', email: '', telefone: '', papel: 'PROPRIETARIO', observacoes: '' }

function MoradoresPage() {
  const [unidades, setUnidades] = useState([])
  const [moradores, setMoradores] = useState([])
  const [loading, setLoading] = useState(true)
  const [formUnidade, setFormUnidade] = useState(INITIAL_UNIDADE)
  const [formMorador, setFormMorador] = useState(INITIAL_MORADOR)
  const [submittingUnidade, setSubmittingUnidade] = useState(false)
  const [submittingMorador, setSubmittingMorador] = useState(false)
  const [editingMorador, setEditingMorador] = useState({})
  const [pendingInativarId, setPendingInativarId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [uRes, mRes] = await Promise.all([
        apiFetch('/api/unidades'),
        apiFetch('/api/moradores'),
      ])
      if (!uRes.ok) throw new Error(await parseError(uRes, 'Falha ao carregar unidades.'))
      if (!mRes.ok) throw new Error(await parseError(mRes, 'Falha ao carregar moradores.'))
      const [u, m] = await Promise.all([parseJson(uRes), parseJson(mRes)])
      setUnidades(u)
      setMoradores(m)
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

  function onUnidadeChange(e) {
    setFormUnidade((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function onMoradorChange(e) {
    setFormMorador((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function onEditChange(id, e) {
    setEditingMorador((prev) => ({
      ...prev,
      [id]: { ...(prev[id] || {}), [e.target.name]: e.target.value },
    }))
  }

  function startEditMorador(m) {
    setEditingMorador((prev) => ({
      ...prev,
      [m.id]: {
        unidadeId: m.unidadeId,
        nome: m.nome,
        email: m.email || '',
        telefone: m.telefone || '',
        papel: m.papel,
        observacoes: m.observacoes || '',
      },
    }))
  }

  async function onSubmitUnidade(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmittingUnidade(true)
    try {
      const res = await apiFetch('/api/unidades', {
        method: 'POST',
        body: JSON.stringify(formUnidade),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao cadastrar unidade.'))
      }
      setSuccess('Unidade cadastrada com sucesso.')
      setFormUnidade(INITIAL_UNIDADE)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmittingUnidade(false)
    }
  }

  async function onSubmitMorador(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmittingMorador(true)
    try {
      const res = await apiFetch('/api/moradores', {
        method: 'POST',
        body: JSON.stringify({ ...formMorador, unidadeId: formMorador.unidadeId || null }),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao cadastrar morador.'))
      }
      setSuccess('Morador cadastrado com sucesso.')
      setFormMorador(INITIAL_MORADOR)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmittingMorador(false)
    }
  }

  async function onUpdateMorador(id) {
    const data = editingMorador[id]
    if (!data?.nome?.trim()) return
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/moradores/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...data, unidadeId: data.unidadeId || null }),
      })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao atualizar morador.'))
      setSuccess('Morador atualizado com sucesso.')
      setEditingMorador((prev) => { const c = { ...prev }; delete c[id]; return c })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onInativar(id) {
    setError('')
    setSuccess('')
    try {
      const res = await apiFetch(`/api/moradores/${id}/inativar`, { method: 'POST' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao inativar morador.'))
      setSuccess('Morador inativado com sucesso.')
      setPendingInativarId(null)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Cadastro de moradores</p>
        <h1>Unidades e Moradores</h1>
        <p className="subtitle">Gerencie as unidades e os moradores do condomínio.</p>
      </section>

      <SuccessState message={success} />

      <div className="two-panel-grid">
        <section className="panel">
          <h2>Nova unidade</h2>
          <form onSubmit={onSubmitUnidade} className="form-grid">
            <label>Bloco<Input name="bloco" value={formUnidade.bloco} onChange={onUnidadeChange} maxLength={30} placeholder="Ex: A, Torre 1..." /></label>
            <label>Número *<Input name="numero" value={formUnidade.numero} onChange={onUnidadeChange} required maxLength={30} /></label>
            <label className="full">Complemento<Input name="complemento" value={formUnidade.complemento} onChange={onUnidadeChange} maxLength={100} /></label>
            <Button type="submit" disabled={submittingUnidade} className="full">
              {submittingUnidade ? 'Salvando...' : 'Cadastrar unidade'}
            </Button>
          </form>
        </section>

        <section className="panel">
          <h2>Novo morador</h2>
          <form onSubmit={onSubmitMorador} className="form-grid">
            <label className="full">
              Unidade *
              <Select name="unidadeId" value={formMorador.unidadeId} onChange={onMoradorChange} required>
                <option value="">Selecione...</option>
                {unidades.map((u) => <option key={u.id} value={u.id}>{u.rotulo}</option>)}
              </Select>
            </label>
            <label>Nome *<Input name="nome" value={formMorador.nome} onChange={onMoradorChange} required maxLength={150} /></label>
            <label>
              Papel
              <Select name="papel" value={formMorador.papel} onChange={onMoradorChange}>
                {PAPEIS.map((p) => <option key={p} value={p}>{p}</option>)}
              </Select>
            </label>
            <label>Email<Input name="email" type="email" value={formMorador.email} onChange={onMoradorChange} maxLength={150} /></label>
            <label>Telefone<Input name="telefone" value={formMorador.telefone} onChange={onMoradorChange} maxLength={30} /></label>
            <label className="full">Observações<Textarea name="observacoes" value={formMorador.observacoes} onChange={onMoradorChange} rows={2} /></label>
            <div className="notice-box full">
              <strong>Aviso LGPD:</strong> Ao cadastrar dados de moradores, prestadores de serviço ou terceiros, declaro que possuo autorização, obrigação legal, relação contratual ou outra base legal adequada para realizar esse cadastro, responsabilizando-me pela exatidão das informações inseridas e pelo uso da plataforma conforme a LGPD.
            </div>
            <Button type="submit" disabled={submittingMorador} className="full">
              {submittingMorador ? 'Salvando...' : 'Cadastrar morador'}
            </Button>
          </form>
        </section>
      </div>

      <section className="board section-spacer">
        <h2 className="board-title">Moradores ativos</h2>
        {loading ? <LoadingState message="Carregando moradores..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={load} /> : null}
        {!loading && !error && moradores.length === 0 ? <EmptyState message="Nenhum morador cadastrado." /> : null}
        {moradores.map((m) => (
          <article key={m.id} className="item">
            {editingMorador[m.id] ? (
              <>
                <label className="full">
                  Unidade
                  <Select name="unidadeId" value={editingMorador[m.id].unidadeId} onChange={(e) => onEditChange(m.id, e)}>
                    {unidades.map((u) => <option key={u.id} value={u.id}>{u.rotulo}</option>)}
                  </Select>
                </label>
                <label>Nome<Input name="nome" value={editingMorador[m.id].nome} onChange={(e) => onEditChange(m.id, e)} /></label>
                <label>
                  Papel
                  <Select name="papel" value={editingMorador[m.id].papel} onChange={(e) => onEditChange(m.id, e)}>
                    {PAPEIS.map((p) => <option key={p} value={p}>{p}</option>)}
                  </Select>
                </label>
                <label>Email<Input name="email" type="email" value={editingMorador[m.id].email} onChange={(e) => onEditChange(m.id, e)} /></label>
                <label>Telefone<Input name="telefone" value={editingMorador[m.id].telefone} onChange={(e) => onEditChange(m.id, e)} /></label>
                <label className="full">Observações<Textarea name="observacoes" value={editingMorador[m.id].observacoes} onChange={(e) => onEditChange(m.id, e)} rows={2} /></label>
                <div className="item-actions">
                  <Button onClick={() => onUpdateMorador(m.id)}>Salvar</Button>
                  <Button variant="secondary" onClick={() => setEditingMorador((prev) => { const c = { ...prev }; delete c[m.id]; return c })}>Cancelar</Button>
                </div>
              </>
            ) : (
              <>
                <h3 className="item-title">{m.nome} <small className="muted">· {m.unidadeRotulo}</small></h3>
                <p className="muted item-meta">{m.papel}{m.telefone ? ` · ${m.telefone}` : ''}{m.email ? ` · ${m.email}` : ''}</p>
                {m.observacoes ? <p className="item-description">{m.observacoes}</p> : null}
                <div className="item-actions">
                  <Button onClick={() => startEditMorador(m)}>Editar</Button>
                  <Button variant="danger" onClick={() => setPendingInativarId(m.id)}>Inativar</Button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>

      <ConfirmDialog
        open={pendingInativarId != null}
        title="Inativar morador"
        message="Deseja inativar este morador? Ele deixará de aparecer na lista de moradores ativos."
        confirmLabel="Inativar"
        onCancel={() => setPendingInativarId(null)}
        onConfirm={() => onInativar(pendingInativarId)}
      />
    </>
  )
}

export default MoradoresPage
