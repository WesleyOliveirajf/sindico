import { useState } from 'react'
import { apiFetch, parseError } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import { TIPOS_GASTO } from './financeiroUtils'

const INITIAL_GASTO = {
  descricao: '',
  tipo: 'OUTROS',
  valor: '',
  dataGasto: '',
  fixo: false,
  parcelado: false,
  parcelaAtual: '',
  parcelaTotal: '',
  observacoes: '',
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
    observacoes: gasto.observacoes || '',
  }
}

// Formulário de criação/edição de gasto. Use com `key` quando o item editado mudar.
function GastoForm({ item = null, onSaved, onCancel }) {
  const editingId = item?.id ?? null
  const [form, setForm] = useState(() => (item ? gastoToForm(item) : INITIAL_GASTO))
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function onChange(e) {
    const { name, value, type, checked } = e.target
    setForm((prev) => {
      const updated = { ...prev, [name]: type === 'checkbox' ? checked : value }
      // Limpar campos de parcela quando desmarcar parcelado
      if (name === 'parcelado' && !checked) {
        updated.parcelaAtual = ''
        updated.parcelaTotal = ''
      }
      return updated
    })
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const payload = {
        ...form,
        valor: form.valor ? Number(form.valor) : null,
        dataGasto: form.dataGasto || null,
        parcelaAtual: form.parcelado && form.parcelaAtual ? Number(form.parcelaAtual) : null,
        parcelaTotal: form.parcelado && form.parcelaTotal ? Number(form.parcelaTotal) : null,
      }
      const res = await apiFetch(editingId ? `/api/gastos/${editingId}` : '/api/gastos', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, editingId ? 'Erro ao atualizar gasto.' : 'Erro ao registrar gasto.'))
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
          {TIPOS_GASTO.map((t) => (
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
        <input type="date" name="dataGasto" value={form.dataGasto} onChange={onChange} required />
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

      <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input
          type="checkbox"
          name="parcelado"
          checked={form.parcelado}
          onChange={onChange}
          style={{ width: 'auto', marginTop: 0 }}
        />
        Gasto parcelado
      </label>

      {form.parcelado && (
        <>
          <label>
            Parcela atual *
            <input
              type="number"
              min="1"
              name="parcelaAtual"
              value={form.parcelaAtual}
              onChange={onChange}
              required
              placeholder="Ex: 3"
            />
          </label>
          <label>
            Total de parcelas *
            <input
              type="number"
              min="1"
              name="parcelaTotal"
              value={form.parcelaTotal}
              onChange={onChange}
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
          value={form.observacoes}
          onChange={onChange}
          rows={2}
          placeholder="Informações adicionais..."
        />
      </label>

      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Registrar gasto'}
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

export default GastoForm
