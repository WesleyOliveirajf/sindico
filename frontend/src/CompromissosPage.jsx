import { useEffect, useMemo, useState } from 'react'
import { parseJson } from './api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const INITIAL_FORM = {
  titulo: '',
  tipo: 'MANUTENCAO',
  local: '',
  inicioEm: '',
  fimEm: '',
  descricao: '',
}

function CompromissosPage() {
  const [form, setForm] = useState(INITIAL_FORM)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const isDateRangeValid = useMemo(() => {
    if (!form.inicioEm || !form.fimEm) return true
    return new Date(form.fimEm) > new Date(form.inicioEm)
  }, [form.fimEm, form.inicioEm])

  async function loadCompromissos() {
    setLoading(true)
    setError('')
    try {
      const response = await fetch(`${API_BASE_URL}/api/compromissos`)
      if (!response.ok) throw new Error('Falha ao carregar compromissos.')
      const data = await parseJson(response)
      setItems(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadCompromissos() }, [])

  function onChange(event) {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }))
  }

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!isDateRangeValid) {
      setError('A data final deve ser maior que a data inicial.')
      return
    }

    setSubmitting(true)
    try {
      const response = await fetch(`${API_BASE_URL}/api/compromissos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })
      if (!response.ok) {
        const data = await response.json().catch(() => null)
        throw new Error(data?.message || 'Nao foi possivel salvar o compromisso.')
      }
      setSuccess('Compromisso criado com sucesso.')
      setForm(INITIAL_FORM)
      await loadCompromissos()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Gestao de condominio</p>
        <h1>Agenda do sindico</h1>
        <p className="subtitle">Cadastre e acompanhe compromissos de manutencao e reuniao.</p>
      </section>

      <section className="layout">
        <article className="panel">
          <h2>Novo compromisso</h2>
          <form onSubmit={onSubmit} className="form-grid">
            <label>Titulo<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
            <label>Tipo<select name="tipo" value={form.tipo} onChange={onChange} required><option value="MANUTENCAO">Manutencao</option><option value="REUNIAO">Reuniao</option></select></label>
            <label>Local<input name="local" value={form.local} onChange={onChange} maxLength={150} /></label>
            <label>Inicio<input type="datetime-local" name="inicioEm" value={form.inicioEm} onChange={onChange} required /></label>
            <label>Fim<input type="datetime-local" name="fimEm" value={form.fimEm} onChange={onChange} required /></label>
            <label className="full">Descricao<textarea name="descricao" value={form.descricao} onChange={onChange} maxLength={2000} rows={4} /></label>
            {error ? <p className="message error">{error}</p> : null}
            {success ? <p className="message success">{success}</p> : null}
            <button type="submit" disabled={submitting} className="submit full">{submitting ? 'Salvando...' : 'Salvar compromisso'}</button>
          </form>
        </article>

        <article className="panel">
          <h2>Proximos compromissos</h2>
          {loading ? <p className="muted">Carregando...</p> : null}
          {!loading && items.length === 0 ? <p className="muted">Nenhum compromisso cadastrado.</p> : null}
          <div className="list">
            {items.map((item) => (
              <article key={item.id} className="item">
                <header><strong>{item.titulo}</strong><span className="tag">{item.tipo}</span></header>
                <p>{new Date(item.inicioEm).toLocaleString('pt-BR')} - {new Date(item.fimEm).toLocaleString('pt-BR')}</p>
                {item.local ? <p className="muted">Local: {item.local}</p> : null}
              </article>
            ))}
          </div>
        </article>
      </section>
    </>
  )
}

export default CompromissosPage
