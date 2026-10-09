import { Link } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { apiFetch, parseError, parseJson } from './api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from './components/PageFeedback'
import Button from './components/ui/Button'
import Modal from './components/ui/Modal'
import AnotacaoForm from './components/AnotacaoForm'
import CompromissoForm from './components/CompromissoForm'
import GastoForm from './components/GastoForm'
import ManutencaoForm from './components/ManutencaoForm'
import MoradorForm from './components/MoradorForm'
import PrestadorForm from './components/PrestadorForm'
import RecebimentoForm from './components/RecebimentoForm'
import ReuniaoForm from './components/ReuniaoForm'
import { statusLabel } from './components/manutencaoUtils'

// Cada entidade abre o mesmo formulário usado na sua página, dentro de um modal.
const MODAL_CONFIG = {
  compromisso: {
    Form: CompromissoForm,
    titulo: { novo: 'Novo lembrete', editar: 'Editar lembrete' },
    sucesso: { criado: 'Lembrete criado com sucesso.', atualizado: 'Lembrete atualizado com sucesso.' },
  },
  manutencao: {
    Form: ManutencaoForm,
    titulo: { novo: 'Nova manutenção', editar: 'Editar manutenção' },
    sucesso: { criado: 'Manutenção registrada com sucesso.', atualizado: 'Manutenção atualizada com sucesso.' },
  },
  reuniao: {
    Form: ReuniaoForm,
    titulo: { novo: 'Nova reunião', editar: 'Editar reunião' },
    sucesso: { criado: 'Reunião registrada com sucesso.', atualizado: 'Reunião atualizada com sucesso.' },
  },
  anotacao: {
    Form: AnotacaoForm,
    titulo: { novo: 'Nova anotação', editar: 'Editar anotação' },
    sucesso: { criado: 'Anotação registrada com sucesso.', atualizado: 'Anotação atualizada com sucesso.' },
  },
  gasto: {
    Form: GastoForm,
    titulo: { novo: 'Novo gasto', editar: 'Editar gasto' },
    sucesso: { criado: 'Gasto registrado com sucesso.', atualizado: 'Gasto atualizado com sucesso.' },
  },
  recebimento: {
    Form: RecebimentoForm,
    titulo: { novo: 'Novo recebimento', editar: 'Novo recebimento' },
    sucesso: { criado: 'Recebimento registrado com sucesso.', atualizado: 'Recebimento registrado com sucesso.' },
  },
  morador: {
    Form: MoradorForm,
    titulo: { novo: 'Novo morador', editar: 'Editar morador' },
    sucesso: { criado: 'Morador cadastrado com sucesso.', atualizado: 'Morador atualizado com sucesso.' },
  },
  prestador: {
    Form: PrestadorForm,
    titulo: { novo: 'Novo prestador', editar: 'Editar prestador' },
    sucesso: { criado: 'Prestador cadastrado com sucesso.', atualizado: 'Prestador atualizado com sucesso.' },
  },
}

const QUICK_ACTIONS = [
  { label: 'Novo gasto', kind: 'gasto' },
  { label: 'Novo recebimento', kind: 'recebimento' },
  { label: 'Nova manutenção', kind: 'manutencao' },
  { label: 'Nova reunião', kind: 'reuniao' },
  { label: 'Novo lembrete', kind: 'compromisso' },
  { label: 'Nova anotação', kind: 'anotacao' },
  { label: 'Novo morador', kind: 'morador' },
  { label: 'Novo prestador', kind: 'prestador' },
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

function isCompromissoPendente(item) {
  return !isConcluido(item) && item?.status !== 'CANCELADO'
}

function isManutencaoAberta(item) {
  return !['CONCLUIDA', 'CANCELADA'].includes(item?.status)
}

function toTime(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

function startOfTodayTime() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return today.getTime()
}

function compromissoSituacao(item) {
  const time = toTime(item.inicioEm || item.dataHora || item.data)
  if (!time) return 'A fazer'

  const today = startOfTodayTime()
  const oneDay = 24 * 60 * 60 * 1000
  if (time < today) return 'Vencido'
  if (time < today + oneDay) return 'Hoje'
  return 'Agendado'
}

function DashboardPage() {
  const [data, setData] = useState({
    gastos: [],
    recebimentos: [],
    compromissos: [],
    manutencoes: [],
    reunioes: [],
    anotacoes: [],
    moradores: [],
    prestadores: [],
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  // null = modal fechado; { kind, item: null } = novo; { kind, item } = edição
  const [modal, setModal] = useState(null)

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
        apiFetch('/api/moradores'),
        apiFetch('/api/prestadores'),
      ])

      const failed = requests.find((res) => !res.ok)
      if (failed) throw new Error(await parseError(failed, 'Falha ao carregar dashboard.'))

      const [gastos, recebimentos, compromissos, manutencoes, reunioes, anotacoes, moradores, prestadores] =
        await Promise.all(requests.map(parseJson))

      setData({ gastos, recebimentos, compromissos, manutencoes, reunioes, anotacoes, moradores, prestadores })
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
    if (!modal) return undefined

    function onKeyDown(event) {
      if (event.key === 'Escape') setModal(null)
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [modal])

  function openModal(kind, item = null) {
    setSuccess('')
    setModal({ kind, item })
  }

  function closeModal() {
    setModal(null)
  }

  async function onFormSaved(result) {
    const config = MODAL_CONFIG[modal?.kind]
    const wasEdit = Boolean(result?.wasEdit)
    setModal(null)
    if (config) setSuccess(wasEdit ? config.sucesso.atualizado : config.sucesso.criado)
    await load()
  }

  const summary = useMemo(() => {
    const totalGastos = data.gastos.reduce((sum, item) => sum + Number(item.valor || 0), 0)
    const totalRecebimentos = data.recebimentos.reduce((sum, item) => sum + Number(item.valor || 0), 0)
    const saldo = totalRecebimentos - totalGastos

    const compromissosAFazer = data.compromissos
      .filter(isCompromissoPendente)
      .sort((a, b) => toTime(a.inicioEm || a.dataHora || a.data) - toTime(b.inicioEm || b.dataHora || b.data))

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
      compromissosAFazer,
      manutencoesAbertas,
      reunioesRecentes,
      anotacoesRecentes,
      atividadesRecentes: [
        ...gastosRecentes.map((item) => ({ kind: 'gasto', item, type: 'Gasto', title: item.descricao, date: item.dataGasto, value: item.valor })),
        ...recebimentosRecentes.map((item) => ({ kind: 'recebimento', item, type: 'Recebimento', title: item.descricao, date: item.dataRecebimento, value: item.valor })),
      ]
        .sort((a, b) => toTime(b.date) - toTime(a.date))
        .slice(0, 5),
    }
  }, [data])

  const ModalForm = modal ? MODAL_CONFIG[modal.kind].Form : null
  const modalTitle = modal
    ? (modal.item ? MODAL_CONFIG[modal.kind].titulo.editar : MODAL_CONFIG[modal.kind].titulo.novo)
    : ''

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Visão geral</p>
        <h1>Dashboard</h1>
        <p className="subtitle">Acompanhe a operação do condomínio e priorize o que precisa de atenção.</p>
      </section>

      <SuccessState message={success} />

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
          </section>

          <section className="dashboard-grid">
            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Lembretes a fazer</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('compromisso')}>Novo</Button>
                  <Link to="/compromissos">Ver lembretes</Link>
                </span>
              </div>
              {summary.compromissosAFazer.length === 0 ? (
                <EmptyState message="Nenhum lembrete em aberto." />
              ) : (
                <div className="dashboard-list">
                  {summary.compromissosAFazer.slice(0, 5).map((item) => (
                    <div key={item.id} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.titulo}</strong>
                        <small>{compromissoSituacao(item)} · {formatDateTime(item.inicioEm || item.dataHora || item.data)}</small>
                      </span>
                      <Button variant="secondary" onClick={() => openModal('compromisso', item)}>Editar</Button>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Manutenções abertas</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('manutencao')}>Novo</Button>
                  <Link to="/manutencoes">Ver todas</Link>
                </span>
              </div>
              {summary.manutencoesAbertas.length === 0 ? (
                <EmptyState message="Nenhuma manutenção aberta." />
              ) : (
                <div className="dashboard-list">
                  {summary.manutencoesAbertas.slice(0, 5).map((item) => (
                    <div key={item.id} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.titulo}</strong>
                        <small>{statusLabel(item.status)}{item.local ? ` · ${item.local}` : ''}</small>
                      </span>
                      <Button variant="secondary" onClick={() => openModal('manutencao', item)}>Editar</Button>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Atividade financeira</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('gasto')}>Gasto</Button>
                  <Button variant="secondary" onClick={() => openModal('recebimento')}>Recebimento</Button>
                  <Link to="/gastos">Ver financeiro</Link>
                </span>
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
                      <span className="dashboard-row-end">
                        <strong>{formatCurrency(item.value)}</strong>
                        {item.kind === 'gasto' ? (
                          <Button variant="secondary" onClick={() => openModal('gasto', item.item)}>Editar</Button>
                        ) : null}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Reuniões recentes</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('reuniao')}>Novo</Button>
                  <Link to="/reunioes">Ver reuniões</Link>
                </span>
              </div>
              {summary.reunioesRecentes.length === 0 ? (
                <EmptyState message="Nenhuma reunião registrada." />
              ) : (
                <div className="dashboard-list">
                  {summary.reunioesRecentes.map((item) => (
                    <div key={item.id} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.titulo}</strong>
                        <small>{item.tipo} · {formatDateTime(item.dataHora)}</small>
                      </span>
                      <Button variant="secondary" onClick={() => openModal('reuniao', item)}>Editar</Button>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Anotações recentes</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('anotacao')}>Novo</Button>
                  <Link to="/anotacoes">Ver anotações</Link>
                </span>
              </div>
              {summary.anotacoesRecentes.length === 0 ? (
                <EmptyState message="Nenhuma anotação registrada." />
              ) : (
                <div className="dashboard-list">
                  {summary.anotacoesRecentes.map((item) => (
                    <div key={item.id} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.titulo}</strong>
                        <small>{item.importancia} {item.categoria ? `· ${item.categoria}` : ''}</small>
                      </span>
                      <Button variant="secondary" onClick={() => openModal('anotacao', item)}>Editar</Button>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Moradores</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('morador')}>Novo</Button>
                  <Link to="/moradores">Ver moradores</Link>
                </span>
              </div>
              {data.moradores.length === 0 ? (
                <EmptyState message="Nenhum morador cadastrado." />
              ) : (
                <div className="dashboard-list">
                  {data.moradores.slice(0, 5).map((item) => (
                    <div key={item.id} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.nome}</strong>
                        <small>{item.unidadeRotulo} · {item.papel}</small>
                      </span>
                      <Button variant="secondary" onClick={() => openModal('morador', item)}>Editar</Button>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel">
              <div className="dashboard-panel-head">
                <h2>Prestadores</h2>
                <span className="dashboard-panel-actions">
                  <Button variant="secondary" onClick={() => openModal('prestador')}>Novo</Button>
                  <Link to="/prestadores">Ver prestadores</Link>
                </span>
              </div>
              {data.prestadores.length === 0 ? (
                <EmptyState message="Nenhum prestador cadastrado." />
              ) : (
                <div className="dashboard-list">
                  {data.prestadores.slice(0, 5).map((item) => (
                    <div key={item.id} className="dashboard-row dashboard-row--split">
                      <span>
                        <strong>{item.nome}</strong>
                        <small>{item.telefone}</small>
                      </span>
                      <Button variant="secondary" onClick={() => openModal('prestador', item)}>Editar</Button>
                    </div>
                  ))}
                </div>
              )}
            </article>

            <article className="panel dashboard-panel dashboard-panel--actions">
              <h2>Atalhos rápidos</h2>
              <div className="quick-actions">
                {QUICK_ACTIONS.map((action) => (
                  <button key={action.label} type="button" className="quick-action" onClick={() => openModal(action.kind)}>
                    {action.label}
                  </button>
                ))}
              </div>
            </article>
          </section>
        </>
      )}

      {modal ? (
        <Modal open title={modalTitle} onClose={closeModal} className="ui-modal--wide">
          <ModalForm item={modal.item} onSaved={onFormSaved} onCancel={closeModal} />
        </Modal>
      ) : null}
    </>
  )
}

export default DashboardPage
