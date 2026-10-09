import { useState } from 'react'
import { apiFetch, parseError } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import Input, { Select, Textarea } from './ui/Input'
import { formatDateIso } from './formUtils'

const INITIAL_FORM = {
  titulo: '',
  descricao: '',
  inicioEm: '',
  local: '',
  tipo: 'OUTROS',
}

function compromissoToForm(item) {
  return {
    titulo: item.titulo || '',
    descricao: item.descricao || '',
    inicioEm: formatDateIso(item.inicioEm),
    local: item.local || '',
    tipo: item.tipo || 'OUTROS',
  }
}

// Formulário de criação/edição de lembrete. Use com `key` quando o item editado mudar.
function CompromissoForm({ item = null, onSaved, onCancel }) {
  const editingId = item?.id ?? null
  const [form, setForm] = useState(() => (item ? compromissoToForm(item) : INITIAL_FORM))
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
      const res = await apiFetch(
        editingId ? `/api/compromissos/${editingId}` : '/api/compromissos',
        {
          method: editingId ? 'PUT' : 'POST',
          body: JSON.stringify(form),
        },
      )
      if (!res.ok) {
        throw new Error(await parseError(res, editingId ? 'Erro ao atualizar lembrete.' : 'Erro ao salvar lembrete.'))
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
      <label className="full">
        Título *
        <Input
          name="titulo"
          value={form.titulo}
          onChange={onChange}
          required
          maxLength={150}
          placeholder="Ex: Vistoria da bomba d'água"
        />
      </label>
      <label>
        Data de início *
        <Input type="date" name="inicioEm" value={form.inicioEm} onChange={onChange} required />
      </label>
      <label>
        Tipo
        <Select name="tipo" value={form.tipo} onChange={onChange}>
          <option value="OUTROS">Outros</option>
          <option value="MANUTENCAO">Manutenção</option>
          <option value="REUNIAO">Reunião</option>
        </Select>
      </label>
      <label className="full">
        Local
        <Input
          name="local"
          value={form.local}
          onChange={onChange}
          maxLength={150}
          placeholder="Ex: Sala de reuniões, Subsolo..."
        />
      </label>
      <label className="full">
        Descrição
        <Textarea
          name="descricao"
          value={form.descricao}
          onChange={onChange}
          rows={2}
          placeholder="Detalhes do lembrete..."
        />
      </label>
      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Criar lembrete'}
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

export default CompromissoForm
