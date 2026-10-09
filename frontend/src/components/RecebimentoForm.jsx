import { useState } from 'react'
import { apiFetch, parseError } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import { TIPOS_RECEBIMENTO } from './financeiroUtils'

const INITIAL_RECEBIMENTO = {
  descricao: '',
  tipo: 'TAXA_CONDOMINIO',
  valor: '',
  dataRecebimento: '',
  observacoes: '',
}

// Somente criação: a API de recebimentos não tem atualização (PUT).
function RecebimentoForm({ onSaved, onCancel }) {
  const [form, setForm] = useState(INITIAL_RECEBIMENTO)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function onChange(e) {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const payload = {
        ...form,
        valor: form.valor ? Number(form.valor) : null,
        dataRecebimento: form.dataRecebimento || null,
      }
      const res = await apiFetch('/api/recebimentos', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, 'Erro ao registrar recebimento.'))
      }
      onSaved()
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
          placeholder="Ex: Taxa condominial - Maio/2026"
        />
      </label>

      <label>
        Tipo *
        <select name="tipo" value={form.tipo} onChange={onChange}>
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
          value={form.valor}
          onChange={onChange}
          required
          placeholder="0,00"
        />
      </label>

      <label>
        Data do recebimento *
        <input type="date" name="dataRecebimento" value={form.dataRecebimento} onChange={onChange} required />
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

      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : 'Registrar recebimento'}
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

export default RecebimentoForm
