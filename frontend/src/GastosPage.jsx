import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson, iaAnalisarGastos } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import ConfirmDialog from './components/ConfirmDialog'
import GastoForm from './components/GastoForm'
import RecebimentoForm from './components/RecebimentoForm'
import { TIPOS_GASTO, TIPOS_RECEBIMENTO } from './components/financeiroUtils'

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

const SUMMARY_VALUE_COLORS = {
  danger: '#dc2626',
  success: '#16a34a',
}

function tipoGastoLabel(value) {
  return TIPOS_GASTO.find((t) => t.value === value)?.label || value
}

function tipoRecebimentoLabel(value) {
  return TIPOS_RECEBIMENTO.find((t) => t.value === value)?.label || value
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

function buildGastoQuery(filtroMes, filtroAno, filtroTipo) {
  const params = new URLSearchParams()
  if (filtroMes) params.set('mes', filtroMes)
  if (filtroAno) params.set('ano', filtroAno)
  if (filtroTipo) params.set('tipo', filtroTipo)
  const qs = params.toString()
  return qs ? `/api/gastos?${qs}` : '/api/gastos'
}

function buildRecebimentoQuery(filtroMes, filtroAno) {
  const params = new URLSearchParams()
  if (filtroMes) params.set('mes', filtroMes)
  if (filtroAno) params.set('ano', filtroAno)
  const qs = params.toString()
  return qs ? `/api/recebimentos?${qs}` : '/api/recebimentos'
}

/* ─── Componente principal ────────────────────────────────────── */
function GastosPage() {
  const currentYear = new Date().getFullYear()
  const currentMonth = String(new Date().getMonth() + 1)

  // Aba ativa: 'gastos' | 'recebimentos'
  const [activeTab, setActiveTab] = useState(() => (
    window.location.hash === '#recebimentos' ? 'recebimentos' : 'gastos'
  ))

  // Gastos
  const gastoFormRef = useRef(null)
  const [editingGasto, setEditingGasto] = useState(null)
  const [gastos, setGastos] = useState([])
  const [gastosLoading, setGastosLoading] = useState(true)

  // Recebimentos
  const [recebimentos, setRecebimentos] = useState([])
  const [recebimentosLoading, setRecebimentosLoading] = useState(true)

  // Estado compartilhado
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingDelete, setPendingDelete] = useState(null) // { type: 'gasto'|'recebimento', id }

  // IA
  const [analiseIA, setAnaliseIA] = useState('')
  const [analiseLoading, setAnaliseLoading] = useState(false)

  // Filtros
  const [filtroMes, setFiltroMes] = useState(currentMonth)
  const [filtroAno, setFiltroAno] = useState(String(currentYear))
  const [filtroTipo, setFiltroTipo] = useState('')

  const anos = Array.from({ length: 7 }, (_, i) => String(currentYear - 5 + i))

  /* ─── Carregamento de dados ─────────────────────────────────── */
  async function loadGastos(mes, ano, tipo) {
    setGastosLoading(true)
    setError('')
    try {
      const res = await apiFetch(buildGastoQuery(mes, ano, tipo))
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar gastos.'))
      setGastos(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setGastosLoading(false)
    }
  }

  async function loadRecebimentos(mes, ano) {
    setRecebimentosLoading(true)
    try {
      const res = await apiFetch(buildRecebimentoQuery(mes, ano))
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar recebimentos.'))
      setRecebimentos(await parseJson(res))
    } catch (err) {
      setError(err.message)
    } finally {
      setRecebimentosLoading(false)
    }
  }

  function loadAll(mes, ano, tipo) {
    loadGastos(mes, ano, tipo)
    loadRecebimentos(mes, ano)
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadAll(filtroMes, filtroAno, filtroTipo)
    }, 0)
    return () => clearTimeout(timer)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  /* ─── Filtros ───────────────────────────────────────────────── */
  function onFiltrar(e) {
    e.preventDefault()
    loadAll(filtroMes, filtroAno, filtroTipo)
  }

  function onLimparFiltros() {
    setFiltroMes('')
    setFiltroAno('')
    setFiltroTipo('')
    loadAll('', '', '')
  }

  /* ─── Formulários ───────────────────────────────────────────── */
  function onEditarGasto(gasto) {
    setActiveTab('gastos')
    setError('')
    setSuccess('')
    setEditingGasto(gasto)
    window.setTimeout(() => {
      gastoFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  async function onGastoSaved({ wasEdit }) {
    setSuccess(wasEdit ? 'Gasto atualizado com sucesso.' : 'Gasto registrado com sucesso.')
    setEditingGasto(null)
    await loadAll(filtroMes, filtroAno, filtroTipo)
  }

  async function onRecebimentoSaved() {
    setSuccess('Recebimento registrado com sucesso.')
    await loadAll(filtroMes, filtroAno, filtroTipo)
  }

  /* ─── Exclusão ──────────────────────────────────────────────── */
  async function onDeletar() {
    if (!pendingDelete) return
    setError('')
    try {
      const endpoint = pendingDelete.type === 'gasto'
        ? `/api/gastos/${pendingDelete.id}`
        : `/api/recebimentos/${pendingDelete.id}`
      const res = await apiFetch(endpoint, { method: 'DELETE' })
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao remover registro.'))
      if (pendingDelete.type === 'gasto' && pendingDelete.id === editingGasto?.id) {
        setEditingGasto(null)
      }
      setPendingDelete(null)
      await loadAll(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    }
  }

  /* ─── Totais ────────────────────────────────────────────────── */
  const totalGastos = gastos.reduce((sum, g) => sum + Number(g.valor || 0), 0)
  const totalRecebimentos = recebimentos.reduce((sum, r) => sum + Number(r.valor || 0), 0)
  const saldoReal = totalRecebimentos - totalGastos
  const isLoading = gastosLoading || recebimentosLoading

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Financeiro</p>
        <h1>Controle de Gasto</h1>
        <p className="subtitle">Gerencie gastos, recebimentos e acompanhe o saldo real do condomínio.</p>
      </section>

      <SuccessState message={success} />

      {/* ─── Painel financeiro ──────────────────────────────────── */}
      {!isLoading && (
        <div className="financial-summary">
          <div className="financial-summary-card financial-summary-card--danger">
            <p className="financial-summary-label">💸 Total de Gastos</p>
            <p className="financial-summary-value" style={{ color: SUMMARY_VALUE_COLORS.danger }}>
              {formatCurrency(totalGastos)}
            </p>
            <p className="financial-summary-meta">
              {gastos.length} {gastos.length === 1 ? 'registro' : 'registros'}
            </p>
          </div>

          <div className="financial-summary-card financial-summary-card--success">
            <p className="financial-summary-label">💰 Total de Recebimentos</p>
            <p className="financial-summary-value" style={{ color: SUMMARY_VALUE_COLORS.success }}>
              {formatCurrency(totalRecebimentos)}
            </p>
            <p className="financial-summary-meta">
              {recebimentos.length} {recebimentos.length === 1 ? 'registro' : 'registros'}
            </p>
          </div>

          <div className={`financial-summary-card financial-summary-card--${saldoReal >= 0 ? 'success' : 'danger'}`}>
            <p className="financial-summary-label">📊 Saldo Real</p>
            <p
              className="financial-summary-value"
              style={{ color: SUMMARY_VALUE_COLORS[saldoReal >= 0 ? 'success' : 'danger'] }}
            >
              {formatCurrency(saldoReal)}
            </p>
            <p className="financial-summary-meta">Recebimentos − Gastos</p>
          </div>
        </div>
      )}

      {/* ─── Filtros ───────────────────────────────────────────── */}
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

          {activeTab === 'gastos' && (
            <label>
              Tipo de gasto
              <select value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value)}>
                <option value="">Todos os tipos</option>
                {TIPOS_GASTO.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </label>
          )}

          <div className="form-actions">
            <button type="submit" className="submit">Filtrar</button>
            <button type="button" className="submit cancel" onClick={onLimparFiltros}>
              Limpar
            </button>
          </div>
        </form>
      </section>

      {/* ─── Abas ──────────────────────────────────────────────── */}
      <div className="page-tab-bar">
        <button
          type="button"
          className={`page-tab${activeTab === 'gastos' ? ' page-tab--active' : ''}`}
          onClick={() => setActiveTab('gastos')}
        >
          💸 Gastos
        </button>
        <button
          type="button"
          className={`page-tab${activeTab === 'recebimentos' ? ' page-tab--active' : ''}`}
          onClick={() => setActiveTab('recebimentos')}
        >
          💰 Recebimentos
        </button>
      </div>

      {/* ━━━━━━━━━━━━━ ABA GASTOS ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
      {activeTab === 'gastos' && (
        <>
          <section className="panel" style={{ marginTop: 20 }} ref={gastoFormRef}>
            <h2>{editingGasto ? 'Editar gasto' : 'Novo gasto'}</h2>
            <GastoForm
              key={editingGasto?.id ?? 'novo'}
              item={editingGasto}
              onSaved={onGastoSaved}
              onCancel={editingGasto ? () => setEditingGasto(null) : undefined}
            />
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

          {/* Resumo de gastos */}
          {!gastosLoading && gastos.length > 0 && (
            <section className="panel" style={{ marginTop: 12 }}>
              <p style={{ margin: 0 }}>
                <strong>{gastos.length}</strong> {gastos.length === 1 ? 'gasto encontrado' : 'gastos encontrados'} &mdash; Total:{' '}
                <strong>{formatCurrency(totalGastos)}</strong>
                {' '}({gastos.filter((g) => g.fixo).length} fixos, {gastos.filter((g) => !g.fixo).length} variáveis)
              </p>
            </section>
          )}

          {/* Listagem de gastos */}
          <section className="board" style={{ marginTop: 12 }}>
            {gastosLoading ? <LoadingState message="Carregando gastos..." /> : null}
            {!gastosLoading && error ? <ErrorState message={error} onRetry={() => loadAll(filtroMes, filtroAno, filtroTipo)} /> : null}
            {!gastosLoading && !error && gastos.length === 0 ? <EmptyState message="Nenhum gasto encontrado para os filtros selecionados." /> : null}
            {gastos.map((g) => (
              <article key={g.id} className="item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                  <div style={{ flex: 1 }}>
                    <h3 style={{ margin: 0 }}>{g.descricao}</h3>
                    <p className="muted" style={{ marginTop: 4 }}>
                      {tipoGastoLabel(g.tipo)}
                      {g.fixo ? ' · Fixo' : ' · Variável'}
                      {g.parcelado && g.parcelaAtual && g.parcelaTotal
                        ? ` · Parcela ${g.parcelaAtual}/${g.parcelaTotal}`
                        : ''}
                      {' · '}{formatDate(g.dataGasto)}
                    </p>
                    <p style={{ marginTop: 6, fontWeight: 600 }}>{formatCurrency(g.valor)}</p>
                    {g.observacoes ? <p className="muted" style={{ marginTop: 4 }}>Obs: {g.observacoes}</p> : null}
                  </div>
                  <div style={{ display: 'flex', gap: 8, flexShrink: 0, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                    <button
                      onClick={() => onEditarGasto(g)}
                      style={{
                        background: 'none',
                        border: '1px solid #2563eb',
                        color: '#2563eb',
                        borderRadius: 4,
                        padding: '4px 10px',
                        cursor: 'pointer',
                        fontSize: 12,
                      }}
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => setPendingDelete({ type: 'gasto', id: g.id })}
                      style={{
                        background: 'none',
                        border: '1px solid #cc3333',
                        color: '#cc3333',
                        borderRadius: 4,
                        padding: '4px 10px',
                        cursor: 'pointer',
                        fontSize: 12,
                      }}
                    >
                      Remover
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </section>
        </>
      )}

      {/* ━━━━━━━━━━━━━ ABA RECEBIMENTOS ━━━━━━━━━━━━━━━━━━━━━━━━━ */}
      {activeTab === 'recebimentos' && (
        <>
          <section className="panel" style={{ marginTop: 20 }}>
            <h2>Novo recebimento</h2>
            <RecebimentoForm onSaved={onRecebimentoSaved} />
          </section>

          {/* Resumo de recebimentos */}
          {!recebimentosLoading && recebimentos.length > 0 && (
            <section className="panel" style={{ marginTop: 12 }}>
              <p style={{ margin: 0 }}>
                <strong>{recebimentos.length}</strong>{' '}
                {recebimentos.length === 1 ? 'recebimento encontrado' : 'recebimentos encontrados'} &mdash; Total:{' '}
                <strong>{formatCurrency(totalRecebimentos)}</strong>
              </p>
            </section>
          )}

          {/* Listagem de recebimentos */}
          <section className="board" style={{ marginTop: 12 }}>
            {recebimentosLoading ? <LoadingState message="Carregando recebimentos..." /> : null}
            {!recebimentosLoading && error ? <ErrorState message={error} onRetry={() => loadAll(filtroMes, filtroAno, filtroTipo)} /> : null}
            {!recebimentosLoading && !error && recebimentos.length === 0 ? <EmptyState message="Nenhum recebimento encontrado para os filtros selecionados." /> : null}
            {recebimentos.map((r) => (
              <article key={r.id} className="item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                  <div style={{ flex: 1 }}>
                    <h3 style={{ margin: 0 }}>{r.descricao}</h3>
                    <p className="muted" style={{ marginTop: 4 }}>
                      {tipoRecebimentoLabel(r.tipo)}
                      {' · '}{formatDate(r.dataRecebimento)}
                    </p>
                    <p style={{ marginTop: 6, fontWeight: 600, color: '#16a34a' }}>{formatCurrency(r.valor)}</p>
                    {r.observacoes ? <p className="muted" style={{ marginTop: 4 }}>Obs: {r.observacoes}</p> : null}
                  </div>
                  <button
                    onClick={() => setPendingDelete({ type: 'recebimento', id: r.id })}
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
      )}

      <ConfirmDialog
        open={pendingDelete != null}
        title={pendingDelete?.type === 'recebimento' ? 'Remover recebimento' : 'Remover gasto'}
        message={
          pendingDelete?.type === 'recebimento'
            ? 'Deseja remover este recebimento? Esta ação não pode ser desfeita.'
            : 'Deseja remover este gasto? Esta ação não pode ser desfeita.'
        }
        confirmLabel="Remover"
        onCancel={() => setPendingDelete(null)}
        onConfirm={onDeletar}
      />
    </>
  )
}

export default GastosPage
