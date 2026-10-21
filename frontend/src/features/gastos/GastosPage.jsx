import { useEffect, useRef, useState } from 'react'
import { apiFetch, parseError, parseJson, iaAnalisarGastos } from '../../shared/api'
import { EmptyState, ErrorState, LoadingState, SuccessState } from '../../shared/components/PageFeedback'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import Modal from '../../shared/components/ui/Modal'

const TIPOS_GASTO = [
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

const TIPOS_RECEBIMENTO = [
  { value: 'TAXA_CONDOMINIO', label: 'Taxa de Condomínio' },
  { value: 'ALUGUEL_AREA',   label: 'Aluguel de Área Comum' },
  { value: 'MULTA',          label: 'Multa' },
  { value: 'RESERVA_FUNDO',  label: 'Reserva / Fundo' },
  { value: 'OUTROS',         label: 'Outros' },
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

const INITIAL_GASTO = {
  descricao: '',
  tipo: 'OUTROS',
  valor: '',
  dataGasto: '',
  fixo: false,
  parcelado: false,
  parcelaAtual: '',
  parcelaTotal: '',
  valorParcela: '',
  observacoes: '',
}

const INITIAL_RECEBIMENTO = {
  descricao: '',
  tipo: 'TAXA_CONDOMINIO',
  valor: '',
  dataRecebimento: '',
  observacoes: '',
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

function buildSaldoQuery(filtroMes, filtroAno) {
  const params = new URLSearchParams()
  if (filtroMes) params.set('mes', filtroMes)
  if (filtroAno) params.set('ano', filtroAno)
  const qs = params.toString()
  return qs ? `/api/financeiro/saldo?${qs}` : '/api/financeiro/saldo'
}

function gastoToForm(gasto) {
  return {
    descricao: gasto.descricao || '',
    tipo: gasto.tipo || 'OUTROS',
    valor: gasto.valor != null ? String(gasto.valor) : '',
    dataGasto: gasto.dataGasto || '',
    fixo: Boolean(gasto.fixo),
    parcelado: Boolean(gasto.parcelado),
    parcelaAtual: gasto.parcelaAtual != null ? String(gasto.parcelaAtual) : '',
    parcelaTotal: gasto.parcelaTotal != null ? String(gasto.parcelaTotal) : '',
    valorParcela: gasto.valorParcela != null ? String(gasto.valorParcela) : '',
    observacoes: gasto.observacoes || '',
  }
}

/* ─── Cards de resumo financeiro (CSS responsivo) ─────────────── */
const SUMMARY_VALUE_COLORS = {
  danger: '#dc2626',
  success: '#16a34a',
}

/* ─── Abas ────────────────────────────────────────────────────── */

/* ─── Componente principal ────────────────────────────────────── */
function GastosPage() {
  const currentYear = new Date().getFullYear()
  const currentMonth = String(new Date().getMonth() + 1)

  // Aba ativa: 'gastos' | 'recebimentos'
  const [activeTab, setActiveTab] = useState(() => (
    window.location.hash === '#recebimentos' ? 'recebimentos' : 'gastos'
  ))

  // Formulário de gasto
  const gastoFormRef = useRef(null)
  const [gastoForm, setGastoForm] = useState(INITIAL_GASTO)
  const [editingGastoId, setEditingGastoId] = useState(null)
  const [gastos, setGastos] = useState([])
  const [gastosLoading, setGastosLoading] = useState(true)
  const [gastoSubmitting, setGastoSubmitting] = useState(false)

  // Formulário de recebimento
  const [recebimentoForm, setRecebimentoForm] = useState(INITIAL_RECEBIMENTO)
  const [recebimentos, setRecebimentos] = useState([])
  const [recebimentosLoading, setRecebimentosLoading] = useState(true)
  const [recebimentoSubmitting, setRecebimentoSubmitting] = useState(false)

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

  // Ajuste de saldo (não altera lançamentos)
  const [saldoResumo, setSaldoResumo] = useState(null)
  const [ajusteModalOpen, setAjusteModalOpen] = useState(false)
  const [ajusteSaldoDesejado, setAjusteSaldoDesejado] = useState('')
  const [ajusteObservacao, setAjusteObservacao] = useState('')
  const [ajusteSubmitting, setAjusteSubmitting] = useState(false)

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

  async function loadSaldoResumo(mes, ano) {
    try {
      const res = await apiFetch(buildSaldoQuery(mes, ano))
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar saldo.'))
      setSaldoResumo(await parseJson(res))
    } catch (err) {
      setError(err.message)
      setSaldoResumo(null)
    }
  }

  function loadAll(mes, ano, tipo) {
    loadGastos(mes, ano, tipo)
    loadRecebimentos(mes, ano)
    void loadSaldoResumo(mes, ano)
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

  function resetGastoForm() {
    setEditingGastoId(null)
    setGastoForm(INITIAL_GASTO)
  }

  function onEditarGasto(gasto) {
    setActiveTab('gastos')
    setError('')
    setSuccess('')
    setEditingGastoId(gasto.id)
    setGastoForm(gastoToForm(gasto))
    window.setTimeout(() => {
      gastoFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)
  }

  /* ─── Formulário de gasto ───────────────────────────────────── */
  function onGastoChange(e) {
    const { name, value, type, checked } = e.target
    setGastoForm((prev) => {
      const updated = { ...prev, [name]: type === 'checkbox' ? checked : value }
      // Limpar campos de parcela quando desmarcar parcelado
      if (name === 'parcelado' && !checked) {
        updated.parcelaAtual = ''
        updated.parcelaTotal = ''
      }
      return updated
    })
  }

  async function onGastoSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setGastoSubmitting(true)
    try {
      const payload = {
        ...gastoForm,
        valor: gastoForm.valor ? Number(gastoForm.valor) : null,
        dataGasto: gastoForm.dataGasto || null,
        parcelaAtual: gastoForm.parcelado && gastoForm.parcelaAtual ? Number(gastoForm.parcelaAtual) : null,
        parcelaTotal: gastoForm.parcelado && gastoForm.parcelaTotal ? Number(gastoForm.parcelaTotal) : null,
        valorParcela: gastoForm.parcelado && gastoForm.valorParcela ? Number(gastoForm.valorParcela) : null,
      }
      const endpoint = editingGastoId ? `/api/gastos/${editingGastoId}` : '/api/gastos'
      const res = await apiFetch(endpoint, {
        method: editingGastoId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, editingGastoId ? 'Erro ao atualizar gasto.' : 'Erro ao registrar gasto.'))
      }
      setSuccess(editingGastoId ? 'Gasto atualizado com sucesso.' : 'Gasto registrado com sucesso.')
      resetGastoForm()
      await loadAll(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    } finally {
      setGastoSubmitting(false)
    }
  }

  async function onMarcarParcela(gastoId, parcela) {
    try {
      const res = await apiFetch(`/api/gastos/${gastoId}/parcelas/${parcela.numero}`, {
        method: 'PATCH',
        body: JSON.stringify({ paga: !parcela.paga }),
      })
      if (!res.ok) throw new Error(await parseError(res, 'Não foi possível atualizar a parcela.'))
      await loadGastos(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    }
  }

  /* ─── Formulário de recebimento ─────────────────────────────── */
  function onRecebimentoChange(e) {
    const { name, value } = e.target
    setRecebimentoForm((prev) => ({ ...prev, [name]: value }))
  }

  async function onRecebimentoSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setRecebimentoSubmitting(true)
    try {
      const payload = {
        ...recebimentoForm,
        valor: recebimentoForm.valor ? Number(recebimentoForm.valor) : null,
        dataRecebimento: recebimentoForm.dataRecebimento || null,
      }
      const res = await apiFetch('/api/recebimentos', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao registrar recebimento.'))
      }
      setSuccess('Recebimento registrado com sucesso.')
      setRecebimentoForm(INITIAL_RECEBIMENTO)
      await loadAll(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    } finally {
      setRecebimentoSubmitting(false)
    }
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
      if (pendingDelete.type === 'gasto' && pendingDelete.id === editingGastoId) {
        resetGastoForm()
      }
      setPendingDelete(null)
      await loadAll(filtroMes, filtroAno, filtroTipo)
    } catch (err) {
      setError(err.message)
    }
  }

  function abrirAjusteSaldo() {
    if (!filtroMes || !filtroAno) {
      setError('Selecione mês e ano nos filtros para ajustar o saldo do período.')
      return
    }
    setError('')
    const saldoAtual = saldoResumo?.saldoReal ?? saldoCalculado
    setAjusteSaldoDesejado(saldoAtual != null ? String(saldoAtual) : '')
    setAjusteObservacao(saldoResumo?.observacao || '')
    setAjusteModalOpen(true)
  }

  async function onSalvarAjusteSaldo(e) {
    e.preventDefault()
    if (!filtroMes || !filtroAno) return
    setError('')
    setSuccess('')
    setAjusteSubmitting(true)
    try {
      const res = await apiFetch('/api/financeiro/saldo-ajuste', {
        method: 'PUT',
        body: JSON.stringify({
          mes: Number(filtroMes),
          ano: Number(filtroAno),
          saldoRealDesejado: Number(ajusteSaldoDesejado),
          observacao: ajusteObservacao.trim() || null,
        }),
      })
      if (!res.ok) throw new Error(await parseError(res, 'Não foi possível salvar o ajuste de saldo.'))
      setSaldoResumo(await parseJson(res))
      setSuccess('Saldo real ajustado. Os lançamentos de gastos e recebimentos não foram alterados.')
      setAjusteModalOpen(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setAjusteSubmitting(false)
    }
  }

  /* ─── Totais ────────────────────────────────────────────────── */
  const totalGastos = saldoResumo?.totalGastos != null
    ? Number(saldoResumo.totalGastos)
    : gastos.reduce((sum, g) => sum + Number(g.parcelado ? g.valorPago || 0 : g.valor || 0), 0)
  const totalRecebimentos = saldoResumo?.totalRecebimentos != null
    ? Number(saldoResumo.totalRecebimentos)
    : recebimentos.reduce((sum, r) => sum + Number(r.valor || 0), 0)
  const saldoCalculado = saldoResumo?.saldoCalculado != null
    ? Number(saldoResumo.saldoCalculado)
    : totalRecebimentos - totalGastos
  const ajusteSaldo = Number(saldoResumo?.ajusteSaldo || 0)
  const saldoReal = saldoResumo?.saldoReal != null ? Number(saldoResumo.saldoReal) : saldoCalculado + ajusteSaldo
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
            <p className="financial-summary-meta">
              {ajusteSaldo !== 0
                ? `Calculado ${formatCurrency(saldoCalculado)} + ajuste ${formatCurrency(ajusteSaldo)}`
                : 'Recebimentos − Gastos'}
            </p>
            <button
              type="button"
              className="submit"
              style={{ marginTop: 10, fontSize: '0.82rem', padding: '6px 12px', width: '100%' }}
              onClick={abrirAjusteSaldo}
            >
              Ajustar saldo
            </button>
          </div>
        </div>
      )}

      <Modal open={ajusteModalOpen} title="Ajuste de saldo" onClose={() => setAjusteModalOpen(false)}>
        <p className="muted" style={{ marginTop: 0 }}>
          Informe o saldo real do caixa (ex.: extrato bancário). Gastos e recebimentos lançados permanecem iguais;
          o sistema guarda apenas a diferença como ajuste do período.
        </p>
        <form onSubmit={onSalvarAjusteSaldo} className="form-grid" style={{ marginTop: 12 }}>
          <label className="full">
            Saldo calculado (lançamentos)
            <input type="text" readOnly value={formatCurrency(saldoCalculado)} />
          </label>
          <label className="full">
            Saldo real desejado (R$) *
            <input
              type="number"
              step="0.01"
              required
              value={ajusteSaldoDesejado}
              onChange={(e) => setAjusteSaldoDesejado(e.target.value)}
              placeholder="0,00"
            />
          </label>
          <label className="full">
            Motivo / observação
            <textarea
              rows={2}
              value={ajusteObservacao}
              onChange={(e) => setAjusteObservacao(e.target.value)}
              placeholder="Ex.: conciliação com extrato, saldo anterior ao sistema..."
            />
          </label>
          <div className="full form-actions">
            <button type="submit" className="submit" disabled={ajusteSubmitting}>
              {ajusteSubmitting ? 'Salvando...' : 'Salvar ajuste'}
            </button>
            <button type="button" className="submit cancel" onClick={() => setAjusteModalOpen(false)}>
              Cancelar
            </button>
          </div>
        </form>
      </Modal>

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
          {/* Formulário de gasto */}
          <section className="panel" style={{ marginTop: 20 }} ref={gastoFormRef}>
            <h2>{editingGastoId ? 'Editar gasto' : 'Novo gasto'}</h2>
            <form onSubmit={onGastoSubmit} className="form-grid">
              <label>
                Descrição *
                <input
                  name="descricao"
                  value={gastoForm.descricao}
                  onChange={onGastoChange}
                  required
                  maxLength={255}
                  placeholder="Ex: Conta de água de maio"
                />
              </label>

              <label>
                Tipo *
                <select name="tipo" value={gastoForm.tipo} onChange={onGastoChange}>
                  {TIPOS_GASTO.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </label>

              <label>
                Valor total da obra (R$) *
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  name="valor"
                  value={gastoForm.valor}
                  onChange={onGastoChange}
                  required
                  placeholder="0,00"
                />
              </label>

              <label>
                Data do gasto *
                <input
                  type="date"
                  name="dataGasto"
                  value={gastoForm.dataGasto}
                  onChange={onGastoChange}
                  required
                />
              </label>

              <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <input
                  type="checkbox"
                  name="fixo"
                  checked={gastoForm.fixo}
                  onChange={onGastoChange}
                  style={{ width: 'auto', marginTop: 0 }}
                />
                Gasto fixo (recorrente todo mês)
              </label>

              <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <input
                  type="checkbox"
                  name="parcelado"
                  checked={gastoForm.parcelado}
                  onChange={onGastoChange}
                  style={{ width: 'auto', marginTop: 0 }}
                />
                Gasto parcelado
              </label>

              {gastoForm.parcelado && (
                <>
                  <label>
                    Parcela atual *
                    <input
                      type="number"
                      min="1"
                      name="parcelaAtual"
                      value={gastoForm.parcelaAtual}
                      onChange={onGastoChange}
                      required
                      placeholder="Ex: 3"
                    />
                  </label>
                  <label>
                    Valor de cada parcela (R$) *
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      name="valorParcela"
                      value={gastoForm.valorParcela}
                      onChange={onGastoChange}
                      required
                      placeholder="Ex: 500,00"
                    />
                  </label>
                  <label>
                    Total de parcelas *
                    <input
                      type="number"
                      min="1"
                      name="parcelaTotal"
                      value={gastoForm.parcelaTotal}
                      onChange={onGastoChange}
                      required
                      placeholder="Ex: 4"
                    />
                  </label>
                </>
              )}

              <label className="full">
                Observações
                <textarea
                  name="observacoes"
                  value={gastoForm.observacoes}
                  onChange={onGastoChange}
                  rows={2}
                  placeholder="Informações adicionais..."
                />
              </label>

              <div className="full" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <button type="submit" disabled={gastoSubmitting} className="submit" style={{ flex: '1 1 220px' }}>
                  {gastoSubmitting
                    ? 'Salvando...'
                    : editingGastoId ? 'Salvar alterações' : 'Registrar gasto'}
                </button>
                {editingGastoId && (
                  <button
                    type="button"
                    className="submit"
                    style={{ flex: '0 1 160px', background: 'var(--color-muted, #888)' }}
                    onClick={resetGastoForm}
                  >
                    Cancelar
                  </button>
                )}
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
                    {g.parcelado && g.parcelas?.length > 0 && (
                      <div style={{ marginTop: 8, display: 'grid', gap: 4 }}>
                        <span className="muted">Pago: {formatCurrency(g.valorPago)} · Saldo: {formatCurrency(g.saldoDevedor)}</span>
                        {g.parcelas.map((parcela) => (
                          <label key={parcela.numero} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
                            <input
                              type="checkbox"
                              checked={parcela.paga}
                              onChange={() => onMarcarParcela(g.id, parcela)}
                              style={{ width: 'auto', marginTop: 0 }}
                            />
                            Parcela {parcela.numero}: {formatCurrency(parcela.valor)}
                          </label>
                        ))}
                      </div>
                    )}
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
          {/* Formulário de recebimento */}
          <section className="panel" style={{ marginTop: 20 }}>
            <h2>Novo recebimento</h2>
            <form onSubmit={onRecebimentoSubmit} className="form-grid">
              <label>
                Descrição *
                <input
                  name="descricao"
                  value={recebimentoForm.descricao}
                  onChange={onRecebimentoChange}
                  required
                  maxLength={255}
                  placeholder="Ex: Taxa condominial - Maio/2026"
                />
              </label>

              <label>
                Tipo *
                <select name="tipo" value={recebimentoForm.tipo} onChange={onRecebimentoChange}>
                  {TIPOS_RECEBIMENTO.map((t) => (
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
                  value={recebimentoForm.valor}
                  onChange={onRecebimentoChange}
                  required
                  placeholder="0,00"
                />
              </label>

              <label>
                Data do recebimento *
                <input
                  type="date"
                  name="dataRecebimento"
                  value={recebimentoForm.dataRecebimento}
                  onChange={onRecebimentoChange}
                  required
                />
              </label>

              <label className="full">
                Observações
                <textarea
                  name="observacoes"
                  value={recebimentoForm.observacoes}
                  onChange={onRecebimentoChange}
                  rows={2}
                  placeholder="Informações adicionais..."
                />
              </label>

              <button type="submit" disabled={recebimentoSubmitting} className="submit full">
                {recebimentoSubmitting ? 'Salvando...' : 'Registrar recebimento'}
              </button>
            </form>
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
