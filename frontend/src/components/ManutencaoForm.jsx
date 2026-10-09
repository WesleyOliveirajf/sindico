import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson, iaTriarManutencao } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import Input, { Select, Textarea } from './ui/Input'
import { STATUS_OPTIONS, formatCurrency, parseCurrencyValue } from './manutencaoUtils'

const INITIAL_FORM = {
  titulo: '',
  descricao: '',
  tipo: 'PREVENTIVA',
  categoria: '',
  local: '',
  fornecedorId: '',
  responsavelInterno: '',
  dataOcorrencia: '',
  dataExecucao: '',
  custoPrevisto: '',
  custoRealizado: '',
  status: 'ABERTA',
  observacoes: '',
}

function manutencaoToForm(manutencao) {
  return {
    titulo: manutencao.titulo || '',
    descricao: manutencao.descricao || '',
    tipo: manutencao.tipo || 'PREVENTIVA',
    categoria: manutencao.categoria || '',
    local: manutencao.local || '',
    fornecedorId: manutencao.fornecedorId || '',
    responsavelInterno: manutencao.responsavelInterno || '',
    dataOcorrencia: manutencao.dataOcorrencia || '',
    dataExecucao: manutencao.dataExecucao || '',
    custoPrevisto: manutencao.custoPrevisto != null ? formatCurrency(manutencao.custoPrevisto) : '',
    custoRealizado: manutencao.custoRealizado != null ? formatCurrency(manutencao.custoRealizado) : '',
    status: manutencao.status || 'ABERTA',
    observacoes: manutencao.observacoes || '',
  }
}

// Formulário de criação/edição de manutenção. Use com `key` quando o item
// editado mudar, para que o estado interno seja recriado.
function ManutencaoForm({ manutencao = null, onSaved, onCancel }) {
  const editingId = manutencao?.id ?? null
  const [form, setForm] = useState(() => (manutencao ? manutencaoToForm(manutencao) : INITIAL_FORM))
  const [prestadores, setPrestadores] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  // IA triagem
  const [triando, setTriando] = useState(false)
  const [triagemMsg, setTriagemMsg] = useState('')

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

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function formatCurrencyField(fieldName) {
    setForm((prev) => ({ ...prev, [fieldName]: formatCurrency(prev[fieldName]) }))
  }

  function onCurrencyBlur(e) {
    formatCurrencyField(e.target.name)
  }

  function onCurrencyKeyDown(e) {
    if (e.key !== 'Enter') return
    e.preventDefault()
    formatCurrencyField(e.target.name)
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const payload = {
        ...form,
        dataOcorrencia: form.dataOcorrencia || null,
        dataExecucao: form.dataExecucao || null,
        custoPrevisto: parseCurrencyValue(form.custoPrevisto),
        custoRealizado: parseCurrencyValue(form.custoRealizado),
        fornecedorId: form.fornecedorId || null,
      }
      const endpoint = editingId ? `/api/manutencoes/${editingId}` : '/api/manutencoes'
      const res = await apiFetch(endpoint, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(
          res,
          editingId ? 'Erro ao atualizar manutenção.' : 'Erro ao registrar manutenção.'
        ))
      }
      onSaved({ wasEdit: Boolean(editingId) })
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={onSubmit} className="form-grid">
      {error ? <Alert variant="error" role="alert" className="full">{error}</Alert> : null}
      <label>Título *<Input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
      <label>Tipo<Select name="tipo" value={form.tipo} onChange={onChange}><option value="PREVENTIVA">Preventiva</option><option value="CORRETIVA">Corretiva</option></Select></label>
      <label>Categoria<Input name="categoria" value={form.categoria} onChange={onChange} maxLength={50} /></label>
      <label>Local<Input name="local" value={form.local} onChange={onChange} maxLength={150} /></label>
      <label>
        Prestador que realizou *
        <Select name="fornecedorId" value={form.fornecedorId} onChange={onChange} required>
          <option value="">Selecione</option>
          {prestadores.map((p) => (
            <option key={p.id} value={p.id}>{p.nome}</option>
          ))}
        </Select>
      </label>
      <label>Responsável interno<Input name="responsavelInterno" value={form.responsavelInterno} onChange={onChange} maxLength={150} /></label>
      <label>
        Status
        <Select name="status" value={form.status} onChange={onChange}>
          {STATUS_OPTIONS.map((status) => (
            <option key={status.value} value={status.value}>{status.label}</option>
          ))}
        </Select>
      </label>
      <label>Data da ocorrência<Input type="date" name="dataOcorrencia" value={form.dataOcorrencia} onChange={onChange} /></label>
      <label>Data da execução<Input type="date" name="dataExecucao" value={form.dataExecucao} onChange={onChange} /></label>
      <label>
        Custo previsto
        <Input
          type="text"
          inputMode="decimal"
          name="custoPrevisto"
          value={form.custoPrevisto}
          onChange={onChange}
          onBlur={onCurrencyBlur}
          onKeyDown={onCurrencyKeyDown}
          placeholder="R$ 0,00"
        />
      </label>
      <label>
        Custo realizado
        <Input
          type="text"
          inputMode="decimal"
          name="custoRealizado"
          value={form.custoRealizado}
          onChange={onChange}
          onBlur={onCurrencyBlur}
          onKeyDown={onCurrencyKeyDown}
          placeholder="R$ 0,00"
        />
      </label>
      <label className="full">Descrição<Textarea name="descricao" value={form.descricao} onChange={onChange} rows={3} placeholder="Descreva o problema ou serviço necessário..." /></label>

      <div className="ai-action-row full">
        <Button
          type="button"
          className="ui-button--ai"
          disabled={triando || !form.descricao.trim()}
          onClick={async () => {
            setTriando(true)
            setTriagemMsg('')
            try {
              const data = await iaTriarManutencao(form.descricao)
              setForm((prev) => ({
                ...prev,
                tipo: data.tipo || prev.tipo,
                categoria: data.categoria || prev.categoria,
                titulo: data.tituloSugerido || prev.titulo,
                observacoes: data.observacoes || prev.observacoes,
              }))
              const urgLabel = data.urgencia ? ` · Urgência: ${data.urgencia}` : ''
              setTriagemMsg(`Triagem concluída: ${data.tipo} · ${data.categoria}${urgLabel}`)
            } catch (err) {
              setTriagemMsg(`Erro: ${err.message}`)
            } finally {
              setTriando(false)
            }
          }}
        >
          {triando ? 'Triando...' : 'Triar com IA'}
        </Button>
        {triagemMsg && <span className="muted ai-action-message">{triagemMsg}</span>}
      </div>

      <label className="full">Observações<Textarea name="observacoes" value={form.observacoes} onChange={onChange} rows={2} /></label>
      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Registrar manutenção'}
        </Button>
        {onCancel ? (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancelar
          </Button>
        ) : null}
      </div>
    </form>
  )
}

export default ManutencaoForm
