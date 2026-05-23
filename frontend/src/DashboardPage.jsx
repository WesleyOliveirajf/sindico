import { Link } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState } from './components/PageFeedback'

const quickActions = [
  { label: 'Novo gasto', to: '/gastos#gastos' },
  { label: 'Novo recebimento', to: '/gastos#recebimentos' },
  { label: 'Nova manutenção', to: '/manutencoes' },
  { label: 'Nova reunião', to: '/reunioes' },
  { label: 'Novo compromisso', to: '/compromissos' },
]

function currentMonthParams() {
  const now = new Date()
  return {
    mes: String(now.getMonth() + 1),
    ano: String(now.getFullYear()),
  }
}

function formatCurrency(value) {
  return Number(value || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDate(value) {
  if (!value) return '-'
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [year, month, day] = value.split('-')
    return `${day}/${month}/${year}`
  }
  return formatDateTime(value)
}

function isConcluido(item) {
  return item?.status === 'CONCLUIDO' || item?.concluido === true
}

function isManutencaoAberta(item) {
  return !['CONCLUIDA', 'CANCELADA'].includes(item?.status)
}

function toTime(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

function DashboardPage() {
  const [data, setData] = useState({
    gastos: [],
    recebimentos: [],
    compromissos: [],
    manutencoes: [],
    reunioes: [],
    anotacoes: [],
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    const { mes, ano } = currentMonthParams()

    try {
      const requests = await Promise.all([
        apiFetch(`/api/gastos?mes=${mes}&ano=${ano}`),
        apiFetch(`/api/recebimentos?mes=${mes}&ano=${ano}`),
        apiFetch('/api/compromissos'),
        apiFetch('/api/manutencoes'),
        apiFetch('/api/reunioes'),
        apiFetch('/api/anotacoes'),
      ])

      const failed = requests.find((res) => !res.ok)
      if (failed) throw new Error(await parseError(failed, 'Falha ao carregar dashboard.'))

      const [gastos, recebimentos, compromissos, manutencoes, reunioes, anotacoes] =
        await Promise.all(requests.map(parseJson))

      setData({ gastos, recebimentos, compromissos, manutencoes, reunioes, anotacoes })
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

  const summary = useMemo(() => {
    const totalGastos = data.gastos.reduce((sum, item) => sum + Number(item.valor || 0), 0)
    const totalRecebimentos = data.recebimentos.reduce((sum, item) => sum + Number(item.valor || 0), 0)
    const saldo = totalRecebimentos - totalGastos
    const now = Date.now()
    const sevenDays = now + 7 * 24 * 60 * 60 * 1000

    const compromissosAbertos = data.compromissos.filter((item) => !isConcluido(item))
    const compromissosProximos = compromissosAbertos
      .filter((item) => {
        const time = toTime(item.inicioEm || item.dataHora || item.data)
        return time >= now && time <= sevenDays
      })
      .sort((a, b) => toTime(a.inicioEm || a.dataHora || a.data) - toTime(b.inicioEm || b.dataHora || b.data))

    const compromissosVencidos = compromissosAbertos.filter((item) => {
      const time = toTime(item.inicioEm || item.dataHora || item.data)
      return time > 0 && time < now
    })

    const manutencoesAbertas = data.manutencoes.filter(isManutencaoAberta)
    const reunioesRecentes = [...data.reunioes]
      .sort((a, b) => toTime(b.dataHora) - toTime(a.dataHora))
      .slice(0, 3)
    const anotacoesRecentes = [...data.anotacoes]
      .sort((a, b) => toTime(b.createdAt || b.dataReferencia) - toTime(a.createdAt || a.dataReferencia))
      .slice(0, 3)
    const gastosRecentes = [...data.gastos]
      .sort((a, b) => toTime(b.dataGasto) - toTime(a.dataGasto))
      .slice(0, 3)
    const recebimentosRecentes = [...data.recebimentos]
      .sort((a, b) => toTime(b.dataRecebimento) - toTime(a.dataRecebimento))
      .slice(0, 3)

    return {
      totalGastos,
      totalRecebimentos,
      saldo,
      compromissosProximos,
      compromissosVencidos,
      manutencoesAbertas,
      reunioesRecentes,
      anotacoesRecentes,
      atividadesRecentes: [
        ...gastosRecentes.map((item) => ({ type: 'Gasto', title: item.descricao, date: item.dataGasto, value: item.valor })),
        ...recebimentosRecentes.map((item) => ({ type: 'Recebimento', title: item.descricao, date: item.dataRecebimento, value: item.valor })),
      ]
        .sort((a, b) => toTime(b.date) - toTime(a.date))
        .slice(0, 5),
    }
  }, [data])

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Visão geral</p>
        <h1>Dashboard</h1>
        <p className="subtitle">Acompanhe a operação do condomínio e priorize o que precisa de atenção.</p>
      </section>

      {loading ? <div style={{ marginTop: 20 }}><LoadingState message="Carregando dashboard..." /></div> : null}
      {!loading && error ? <div style={{ marginTop: 20 }}><ErrorState message={error} onRetry={load} /></div> : null}

      {!loading && !error && (
        <>
          <section className="dashboard-metrics" aria-label="Indicadores principais">
            <article className="metric-card metric-card--danger">
              <span className="metric-label">Gastos do mês</span>
              <strong>{formatCurrency(summary.totalGastos)}</strong>
              <small>{data.gastos.length} registros</small>
            </article>
            <article className="metric-card metric-card--success">
              <span className="metric-label">Recebimentos do mês</span>
              <strong>{formatCurrency(summary.totalRecebimentos)}</strong>
              <small>{data.recebimentos.length} registros</small>
            </article>
            <article className={`metric-card ${summary.saldo < 0 ? 'metric-card--danger' : 'metric-card--success'}`}>
              <span className="metric-label">Saldo real do mês</span>
              <strong>{formatCurrency(summary.saldo)}</strong>
              <small>{summary.saldo < 0 ? 'Atenção ao caixa' : 'Caixa positivo'}</small>
            </article>
            <article className="metric-card metric-card--warning">
              <span className="metric-label">Pendências</span>
              <strong>{summary.compromissosVencidos.length + summary.manutencoesAbertas.length}</strong>
              <small>vencidas ou abertas</small>
            </article>
          </section>

          <section className="dashboard-grid">
            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Próximos compromissos</h2>
                <Link to="/compromissos">Ver agenda</Link>
              </div>
              {summary.compromissosProximos.length === 0 ? (
                <EmptyState message="Nenhum compromisso nos próximos 7 dias." />
              ) : (
                <div className="dashboard-list">
                  {summary.compromissosProximos.slice(0, 5).map((item) => (
                    <div key={item.id} className="dashboard-row">
                      <strong>{item.titulo}</strong>
                      <span>{formatDateTime(item.inicioEm || item.dataHora || item.data)}</span>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Manutenções abertas</h2>
                <Link to="/manutencoes">Ver manutenções</Link>
              </div>
              {summary.manutencoesAbertas.length === 0 ? (
                <EmptyState message="Nenhuma manutenção aberta." />
              ) : (
                <div className="dashboard-list">
                  {summary.manutencoesAbertas.slice(0, 5).map((item) => (
                    <div key={item.id} className="dashboard-row">
                      <strong>{item.titulo}</strong>
                      <span>{item.status} {item.local ? `· ${item.local}` : ''}</span>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Atividade financeira</h2>
                <Link to="/gastos">Ver financeiro</Link>
              </div>
              {summary.atividadesRecentes.length === 0 ? (
                <EmptyState message="Nenhuma movimentação financeira no mês." />
              ) : (
                <div className="dashboard-list">
                  {summary.atividadesRecentes.map((item, index) => (
                    <div key={`${item.type}-${index}`} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.title}</strong>
                        <small>{item.type} · {formatDate(item.date)}</small>
                      </span>
                      <strong>{formatCurrency(item.value)}</strong>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Reuniões recentes</h2>
                <Link to="/reunioes">Ver reuniões</Link>
              </div>
              {summary.reunioesRecentes.length === 0 ? (
                <EmptyState message="Nenhuma reunião registrada." />
              ) : (
                <div className="dashboard-list">
                  {summary.reunioesRecentes.map((item) => (
                    <div key={item.id} className="dashboard-row">
                      <strong>{item.titulo}</strong>
                      <span>{item.tipo} · {formatDateTime(item.dataHora)}</span>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Anotações recentes</h2>
                <Link to="/anotacoes">Ver anotações</Link>
              </div>
              {summary.anotacoesRecentes.length === 0 ? (
                <EmptyState message="Nenhuma anotação registrada." />
              ) : (
                <div className="dashboard-list">
                  {summary.anotacoesRecentes.map((item) => (
                    <div key={item.id} className="dashboard-row">
                      <strong>{item.titulo}</strong>
                      <span>{item.importancia} {item.categoria ? `· ${item.categoria}` : ''}</span>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel dashboard-panel--actions">
              <h2>Atalhos rápidos</h2>
              <div className="quick-actions">
                {quickActions.map((action) => (
                  <Link key={action.label} to={action.to} className="quick-action">
                    {action.label}
                  </Link>
                ))}
              </div>
            </article>
          </section>
        </>
      )}
    </>
  )
}

export default DashboardPage
