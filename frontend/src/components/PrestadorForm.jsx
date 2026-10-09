import { useState } from 'react'
import { apiFetch, parseError } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import Input, { Textarea } from './ui/Input'

const INITIAL_FORM = { nome: '', telefone: '', areaAtuacao: '' }

function prestadorToForm(item) {
  return {
    nome: item.nome || '',
    telefone: item.telefone || '',
    areaAtuacao: item.historicoServicos || '',
  }
}

// Formulário de criação/edição de prestador. Use com `key` quando o item editado mudar.
function PrestadorForm({ item = null, onSaved, onCancel }) {
  const editingId = item?.id ?? null
  const [form, setForm] = useState(() => (item ? prestadorToForm(item) : INITIAL_FORM))
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
      const res = await apiFetch(editingId ? `/api/prestadores/${editingId}` : '/api/prestadores', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify({
          nome: form.nome,
          telefone: form.telefone,
          historicoServicos: form.areaAtuacao,
        }),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, editingId ? 'Erro ao atualizar prestador.' : 'Erro ao cadastrar prestador.'))
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
      <label>Nome *<Input name="nome" value={form.nome} onChange={onChange} required maxLength={150} /></label>
      <label>Telefone *<Input name="telefone" value={form.telefone} onChange={onChange} required maxLength={30} placeholder="(11) 99999-0000" /></label>
      <label className="full">Área de atuação<Textarea name="areaAtuacao" value={form.areaAtuacao} onChange={onChange} rows={3} maxLength={4000} placeholder="Ex: Hidráulica, elétrica predial, manutenção de bombas." /></label>
      <div className="notice-box full">
        <strong>Aviso LGPD:</strong> Ao cadastrar dados de moradores, prestadores de serviço ou terceiros, declaro que possuo autorização, obrigação legal, relação contratual ou outra base legal adequada para realizar esse cadastro, responsabilizando-me pela exatidão das informações inseridas e pelo uso da plataforma conforme a LGPD.
      </div>
      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Cadastrar prestador'}
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

export default PrestadorForm
