import { useState } from 'react'
import { apiFetch, parseError } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import { formatDateIso } from './formUtils'

const IMPORTANCIAS = ['NORMAL', 'IMPORTANTE', 'CRITICO']

const INITIAL_FORM = { titulo: '', categoria: '', descricao: '', referencia: '', importancia: 'NORMAL', dataReferencia: '' }

function anotacaoToForm(item) {
  return {
    titulo: item.titulo || '',
    categoria: item.categoria || '',
    descricao: item.descricao || '',
    referencia: item.referencia || '',
    importancia: item.importancia || 'NORMAL',
    dataReferencia: formatDateIso(item.dataReferencia),
  }
}

// Formulário de criação/edição de anotação. Use com `key` quando o item editado mudar.
function AnotacaoForm({ item = null, onSaved, onCancel }) {
  const editingId = item?.id ?? null
  const [form, setForm] = useState(() => (item ? anotacaoToForm(item) : INITIAL_FORM))
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const payload = {
        titulo: form.titulo,
        categoria: form.categoria || null,
        descricao: form.descricao || null,
        referencia: form.referencia || null,
        importancia: form.importancia,
        dataReferencia: form.dataReferencia || null,
      }
      const res = await apiFetch(editingId ? `/api/anotacoes/${editingId}` : '/api/anotacoes', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, editingId ? 'Erro ao atualizar anotação.' : 'Erro ao registrar anotação.'))
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
      <label>Título *<input name="titulo" value={form.titulo} onChange={onChange} required maxLength={150} /></label>
      <label>Categoria<input name="categoria" value={form.categoria} onChange={onChange} maxLength={50} placeholder="Ex: Manutenção, Financeiro..." /></label>
      <label>
        Data da ocorrência (opcional)
        <input type="date" name="dataReferencia" value={form.dataReferencia} onChange={onChange} />
      </label>
      <label>
        Importância
        <select name="importancia" value={form.importancia} onChange={onChange}>
          {IMPORTANCIAS.map((i) => <option key={i} value={i}>{i}</option>)}
        </select>
      </label>
      <label>Referência<input name="referencia" value={form.referencia} onChange={onChange} maxLength={200} placeholder="Ex: nº documento, protocolo..." /></label>
      <label className="full">Descrição<textarea name="descricao" value={form.descricao} onChange={onChange} rows={3} /></label>
      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Registrar anotação'}
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

export default AnotacaoForm
