import { useState } from 'react'
import { apiFetch, parseError } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import { formatDateTimeLocal } from './formUtils'

const INITIAL_FORM = {
  titulo: '',
  tipo: 'ORDINARIA',
  dataHora: '',
  local: '',
  link: '',
  pauta: '',
  resumo: '',
  decisoes: '',
  pendenciasGeradas: '',
  participantesTexto: '',
}

function reuniaoToForm(item) {
  return {
    titulo: item.titulo || '',
    tipo: item.tipo || 'ORDINARIA',
    dataHora: formatDateTimeLocal(item.dataHora),
    local: item.local || '',
    link: item.link || '',
    pauta: item.pauta || '',
    resumo: item.resumo || '',
    decisoes: item.decisoes || '',
    pendenciasGeradas: item.pendenciasGeradas || '',
    participantesTexto: (item.participantes || []).map((p) => p.nome).join('\n'),
  }
}

// Formulário de criação/edição de reunião. Use com `key` quando o item editado mudar.
function ReuniaoForm({ item = null, onSaved, onCancel }) {
  const editingId = item?.id ?? null
  const [form, setForm] = useState(() => (item ? reuniaoToForm(item) : INITIAL_FORM))
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
      const participantes = form.participantesTexto
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean)
        .map((nome) => ({ nome, presente: true }))

      const payload = {
        titulo: form.titulo,
        tipo: form.tipo,
        dataHora: form.dataHora || null,
        local: form.local,
        link: form.link,
        pauta: form.pauta,
        resumo: form.resumo,
        decisoes: form.decisoes,
        pendenciasGeradas: form.pendenciasGeradas,
        participantes,
      }

      const res = await apiFetch(editingId ? `/api/reunioes/${editingId}` : '/api/reunioes', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, editingId ? 'Erro ao atualizar reunião.' : 'Erro ao registrar reunião.'))
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
      <label>Tipo<select name="tipo" value={form.tipo} onChange={onChange}><option value="ORDINARIA">Ordinária</option><option value="EXTRAORDINARIA">Extraordinária</option><option value="CONSELHO">Conselho</option><option value="ASSEMBLEIA">Assembleia</option></select></label>
      <label>Data e horário *<input type="datetime-local" name="dataHora" value={form.dataHora} onChange={onChange} required /></label>
      <label>Local<input name="local" value={form.local} onChange={onChange} maxLength={150} /></label>
      <label className="full">Link<input name="link" value={form.link} onChange={onChange} maxLength={500} /></label>
      <label className="full">Pauta<textarea name="pauta" value={form.pauta} onChange={onChange} rows={2} /></label>
      <label className="full">Resumo<textarea name="resumo" value={form.resumo} onChange={onChange} rows={2} /></label>
      <label className="full">Decisões<textarea name="decisoes" value={form.decisoes} onChange={onChange} rows={2} /></label>
      <label className="full">Pendências geradas<textarea name="pendenciasGeradas" value={form.pendenciasGeradas} onChange={onChange} rows={2} /></label>
      <label className="full">Participantes (1 por linha)<textarea name="participantesTexto" value={form.participantesTexto} onChange={onChange} rows={3} placeholder="Maria Silva\nJoão Souza" /></label>
      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Registrar reunião'}
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

export default ReuniaoForm
