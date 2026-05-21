import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson, iaAnalisarGastos } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'

const TIPOS = [
  { value: 'AGUA',          label: 'Água' },
  { value: 'LUZ',           label: 'Luz / Energia' },
  { value: 'GAS',           label: 'Gás' },
  { value: 'SEGURO',        label: 'Seguro' },
  { value: 'LIMPEZA',       label: 'Limpeza' },
  { value: 'MANUTENCAO',    label: 'Manutenção' },
  { value: 'ADMINISTRACAO', label: 'Administração' },
  { value: 'SALARIOS',      label: 'Salários / Funcionários' },
  { value: 'IMPOSTOS',      label: 'Impostos / Taxas' },
  { value: 'OUTROS',        label: 'Outros' },
]

const MESES = [
  { value: '1',  label: 'Janeiro' },
  { value: '2',  label: 'Fevereiro' },
  { value: '3',  label: 'Março' },
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
  const [pendingDeleteId, setPendingDeleteId] = useState(null)

  // IA
  const [analiseIA, setAnaliseIA] = useState('')
  const [analiseLoading, setAnaliseLoading] = useState(false)

  // filtros
  const [filtroMes, setFiltroMes] = useState(currentMonth)
  const [filtroAno, setFiltroAno] = useState(String(currentYear))
  const [filtroTipo, setFiltroTipo] = useState('')

  async function load(mes, ano, tipo) {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch(buildQuery(mes, ano, tipo))
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar gastos.'))
      setItems(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void load(filtroMes, filtroAno, filtroTipo)
    }, 0)
    return () => clearTimeout(timer)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

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
        throw new Error(await parseError(res, 'Erro ao registrar gasto.'))
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
    setError('')
    try {
      const res = await apiFetch(`/api/gastos/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao remover gasto.'))
      setPendingDeleteId(null)
      await load(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    }
  }

  const total = items.reduce((sum, g) => sum + Number(g.valor || 0), 0)

  // anos disponíveis para o filtro (5 anos para trás + atual + próximo)
  const anos = Array.from({ length: 7 }, (_, i) => String(currentYear - 5 + i))

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Financeiro</p>
        <h1>Gastos do Condomínio</h1>
        <p className="subtitle">Registre e acompanhe todos os gastos fixos e variáveis do condomínio.</p>
      </section>

      <SuccessState message={success} />

      {/* Formulário de cadastro */}
      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Novo gasto</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <label>
            Descrição *
            <input
              name="descricao"
              value={form.descricao}
              onChange={onChange}
              required
              maxLength={255}
              placeholder="Ex: Conta de água de maio"
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
            Gasto fixo (recorrente todo mês)
          </label>

          <label className="full">
            Observações
            <textarea
              name="observacoes"
              value={form.observacoes}
              onChange={onChange}
              rows={2}
              placeholder="Informações adicionais..."
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
            Mês
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

      {/* Análise IA */}
      <section className="panel" style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <h2 style={{ margin: 0, flex: 1 }}>Análise com IA</h2>
          <button
            className="submit"
            style={{ fontSize: '0.82rem', padding: '7px 14px', background: '#6d28d9' }}
            disabled={analiseLoading}
            onClick={async () => {
              setAnaliseLoading(true)
              setAnaliseIA('')
              try {
                const filtros = {}
                if (filtroMes) filtros.mes = Number(filtroMes)
                if (filtroAno) filtros.ano = Number(filtroAno)
                const data = await iaAnalisarGastos(filtros)
                setAnaliseIA(data.analise)
              } catch (err) {
                setAnaliseIA(`Erro: ${err.message}`)
              } finally {
                setAnaliseLoading(false)
              }
            }}
          >
            {analiseLoading ? 'Analisando...' : 'Analisar gastos com IA'}
          </button>
        </div>
        {analiseIA && (
          <div className="ia-result-box" style={{ marginTop: 12 }}>
            <pre className="ia-result-text">{analiseIA}</pre>
          </div>
        )}
      </section>

      {/* Resumo */}
      {!loading && items.length > 0 && (
        <section className="panel" style={{ marginTop: 12 }}>
          <p style={{ margin: 0 }}>
            <strong>{items.length}</strong> {items.length === 1 ? 'gasto encontrado' : 'gastos encontrados'} &mdash; Total:{' '}
            <strong>{formatCurrency(total)}</strong>
            {' '}({items.filter((g) => g.fixo).length} fixos, {items.filter((g) => !g.fixo).length} variáveis)
          </p>
        </section>
      )}

      {/* Listagem */}
      <section className="board" style={{ marginTop: 12 }}>
        {loading ? <LoadingState message="Carregando gastos..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={() => load(filtroMes, filtroAno, filtroTipo)} /> : null}
        {!loading && !error && items.length === 0 ? <EmptyState message="Nenhum gasto encontrado para os filtros selecionados." /> : null}
        {items.map((g) => (
          <article key={g.id} className="item">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
              <div style={{ flex: 1 }}>
                <h3 style={{ margin: 0 }}>{g.descricao}</h3>
                <p className="muted" style={{ marginTop: 4 }}>
                  {tipoLabel(g.tipo)}
                  {g.fixo ? ' · Fixo' : ' · Variável'}
                  {' · '}{formatDate(g.dataGasto)}
                </p>
                <p style={{ marginTop: 6, fontWeight: 600 }}>{formatCurrency(g.valor)}</p>
                {g.observacoes ? <p className="muted" style={{ marginTop: 4 }}>Obs: {g.observacoes}</p> : null}
              </div>
              <button
                onClick={() => setPendingDeleteId(g.id)}
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

      <ConfirmDialog
        open={pendingDeleteId != null}
        title="Remover gasto"
        message="Deseja remover este gasto? Esta ação não pode ser desfeita."
        confirmLabel="Remover"
        onCancel={() => setPendingDeleteId(null)}
        onConfirm={() => onDeletar(pendingDeleteId)}
      />
    </>
  )
}

export default GastosPage
