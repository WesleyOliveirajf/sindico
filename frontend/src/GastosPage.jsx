import { useEffect, useState } from 'react'
import { apiFetch, parseJson } from './api'

const TIPOS = [
  { value: 'AGUA',          label: 'Agua' },
  { value: 'LUZ',           label: 'Luz / Energia' },
  { value: 'GAS',           label: 'Gas' },
  { value: 'SEGURO',        label: 'Seguro' },
  { value: 'LIMPEZA',       label: 'Limpeza' },
  { value: 'MANUTENCAO',    label: 'Manutencao' },
  { value: 'ADMINISTRACAO', label: 'Administracao' },
  { value: 'SALARIOS',      label: 'Salarios / Funcionarios' },
  { value: 'IMPOSTOS',      label: 'Impostos / Taxas' },
  { value: 'OUTROS',        label: 'Outros' },
]

const MESES = [
  { value: '1',  label: 'Janeiro' },
  { value: '2',  label: 'Fevereiro' },
  { value: '3',  label: 'Marco' },
  { value: '4',  label: 'Abril' },
  { value: '5',  label: 'Maio' },
  { value: '6',  label: 'Junho' },
  { value: '7',  label: 'Julho' },
  { value: '8',  label: 'Agosto' },
  { value: '9',  label: 'Setembro' },
  { value: '10', label: 'Outubro' },
  { value: '11', label: 'Novembro' },
  { value: '12', label: 'Dezembro' },
]

const INITIAL_FORM = {
  descricao: '',
  tipo: 'OUTROS',
  valor: '',
  dataGasto: '',
  fixo: false,
  observacoes: '',
}

function tipoLabel(value) {
  return TIPOS.find((t) => t.value === value)?.label || value
}

function formatCurrency(value) {
  if (value == null) return '-'
  return Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const [year, month, day] = dateStr.split('-')
  return `${day}/${month}/${year}`
}

function buildQuery(filtroMes, filtroAno, filtroTipo) {
  const params = new URLSearchParams()
  if (filtroMes) params.set('mes', filtroMes)
  if (filtroAno) params.set('ano', filtroAno)
  if (filtroTipo) params.set('tipo', filtroTipo)
  const qs = params.toString()
  return qs ? `/api/gastos?${qs}` : '/api/gastos'
}

function GastosPage() {
  const currentYear = new Date().getFullYear()
  const currentMonth = String(new Date().getMonth() + 1)

  const [form, setForm] = useState(INITIAL_FORM)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // filtros
  const [filtroMes, setFiltroMes] = useState(currentMonth)
  const [filtroAno, setFiltroAno] = useState(String(currentYear))
  const [filtroTipo, setFiltroTipo] = useState('')

  async function load(mes, ano, tipo) {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch(buildQuery(mes, ano, tipo))
      if (!res.ok) throw new Error('Falha ao carregar gastos.')
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(filtroMes, filtroAno, filtroTipo)
  }, [])

  function onFiltrar(e) {
    e.preventDefault()
    load(filtroMes, filtroAno, filtroTipo)
  }

  function onLimparFiltros() {
    setFiltroMes('')
    setFiltroAno('')
    setFiltroTipo('')
    load('', '', '')
  }

  function onChange(e) {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const payload = {
        ...form,
        valor: form.valor ? Number(form.valor) : null,
        dataGasto: form.dataGasto || null,
      }
      const res = await apiFetch('/api/gastos', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || 'Erro ao registrar gasto.')
      }
      setSuccess('Gasto registrado com sucesso.')
      setForm(INITIAL_FORM)
      await load(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function onDeletar(id) {
    if (!window.confirm('Deseja remover este gasto?')) return
    setError('')
    try {
      const res = await apiFetch(`/api/gastos/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Erro ao remover gasto.')
      await load(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    }
  }

  const total = items.reduce((sum, g) => sum + Number(g.valor || 0), 0)

  // anos disponiveis para o filtro (5 anos para tras + atual + proximo)
  const anos = Array.from({ length: 7 }, (_, i) => String(currentYear - 5 + i))

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Financeiro</p>
        <h1>Gastos do Condominio</h1>
        <p className="subtitle">Registre e acompanhe todos os gastos fixos e variaveis do condominio.</p>
      </section>

      {error ? <p className="message error">{error}</p> : null}
      {success ? <p className="message success">{success}</p> : null}

      {/* Formulario de cadastro */}
      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Novo gasto</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>
            Descricao *
            <input
              name="descricao"
              value={form.descricao}
              onChange={onChange}
              required
              maxLength={255}
              placeholder="Ex: Conta de agua de maio"
            />
          </label>

          <label>
            Tipo *
            <select name="tipo" value={form.tipo} onChange={onChange}>
              {TIPOS.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>

          <label>
            Valor (R$) *
            <input
              type="number"
              step="0.01"
              min="0.01"
              name="valor"
              value={form.valor}
              onChange={onChange}
              required
              placeholder="0,00"
            />
          </label>

          <label>
            Data do gasto *
            <input
              type="date"
              name="dataGasto"
              value={form.dataGasto}
              onChange={onChange}
              required
            />
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input
              type="checkbox"
              name="fixo"
              checked={form.fixo}
              onChange={onChange}
              style={{ width: 'auto', marginTop: 0 }}
            />
            Gasto fixo (recorrente todo mes)
          </label>

          <label className="full">
            Observacoes
            <textarea
              name="observacoes"
              value={form.observacoes}
              onChange={onChange}
              rows={2}
              placeholder="Informacoes adicionais..."
            />
          </label>

          <button type="submit" disabled={submitting} className="submit full">
            {submitting ? 'Salvando...' : 'Registrar gasto'}
          </button>
        </form>
      </section>

      {/* Filtros */}
      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Filtros</h2>
        <form onSubmit={onFiltrar} className="form-grid">
          <label>
            Mes
            <select value={filtroMes} onChange={(e) => setFiltroMes(e.target.value)}>
              <option value="">Todos os meses</option>
              {MESES.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </label>

          <label>
            Ano
            <select value={filtroAno} onChange={(e) => setFiltroAno(e.target.value)}>
              <option value="">Todos os anos</option>
              {anos.map((a) => (
                <option key={a} value={a}>{a}</option>
              ))}
            </select>
          </label>

          <label>
            Tipo
            <select value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value)}>
              <option value="">Todos os tipos</option>
              {TIPOS.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>

          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
            <button type="submit" className="submit">Filtrar</button>
            <button type="button" className="submit" style={{ background: 'var(--color-muted, #888)' }} onClick={onLimparFiltros}>
              Limpar
            </button>
          </div>
        </form>
      </section>

      {/* Resumo */}
      {!loading && items.length > 0 && (
        <section className="panel" style={{ marginTop: 12 }}>
          <p style={{ margin: 0 }}>
            <strong>{items.length}</strong> {items.length === 1 ? 'gasto encontrado' : 'gastos encontrados'} &mdash; Total:{' '}
            <strong>{formatCurrency(total)}</strong>
            {' '}({items.filter((g) => g.fixo).length} fixos, {items.filter((g) => !g.fixo).length} variaveis)
          </p>
        </section>
      )}

      {/* Listagem */}
      <section className="board" style={{ marginTop: 12 }}>
        {loading ? <p className="muted">Carregando...</p> : null}
        {!loading && items.length === 0 ? (
          <p className="muted">Nenhum gasto encontrado para os filtros selecionados.</p>
        ) : null}
        {items.map((g) => (
          <article key={g.id} className="item">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
              <div style={{ flex: 1 }}>
                <h3 style={{ margin: 0 }}>{g.descricao}</h3>
                <p className="muted" style={{ marginTop: 4 }}>
                  {tipoLabel(g.tipo)}
                  {g.fixo ? ' · Fixo' : ' · Variavel'}
                  {' · '}{formatDate(g.dataGasto)}
                </p>
                <p style={{ marginTop: 6, fontWeight: 600 }}>{formatCurrency(g.valor)}</p>
                {g.observacoes ? <p className="muted" style={{ marginTop: 4 }}>Obs: {g.observacoes}</p> : null}
              </div>
              <button
                onClick={() => onDeletar(g.id)}
                style={{
                  background: 'none',
                  border: '1px solid #cc3333',
                  color: '#cc3333',
                  borderRadius: 4,
                  padding: '4px 10px',
                  cursor: 'pointer',
                  fontSize: 12,
                  flexShrink: 0,
                }}
              >
                Remover
              </button>
            </div>
          </article>
        ))}
      </section>
    </>
  )
}

export default GastosPage
