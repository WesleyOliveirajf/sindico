import { useEffect, useState } from 'react'
import { apiFetch, parseError, parseJson } from '../api'
import Alert from './ui/Alert'
import Button from './ui/Button'
import Input, { Select, Textarea } from './ui/Input'

const PAPEIS = ['PROPRIETARIO', 'INQUILINO', 'DEPENDENTE', 'ZELADOR', 'OUTRO']

const INITIAL_MORADOR = { unidadeId: '', nome: '', email: '', telefone: '', papel: 'PROPRIETARIO', observacoes: '' }

function moradorToForm(item) {
  return {
    unidadeId: item.unidadeId || '',
    nome: item.nome || '',
    email: item.email || '',
    telefone: item.telefone || '',
    papel: item.papel || 'PROPRIETARIO',
    observacoes: item.observacoes || '',
  }
}

// Formulário de criação/edição de morador. Carrega as unidades ao montar;
// use `key` para recarregá-las após cadastrar uma unidade nova.
function MoradorForm({ item = null, onSaved, onCancel }) {
  const editingId = item?.id ?? null
  const [form, setForm] = useState(() => (item ? moradorToForm(item) : INITIAL_MORADOR))
  const [unidades, setUnidades] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadUnidades() {
      try {
        const res = await apiFetch('/api/unidades')
        if (!res.ok) return
        setUnidades(await parseJson(res))
      } catch {
        setUnidades([])
      }
    }
    loadUnidades()
  }, [])

  function onChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const res = await apiFetch(editingId ? `/api/moradores/${editingId}` : '/api/moradores', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify({ ...form, unidadeId: form.unidadeId || null }),
      })
      if (!res.ok) {
        throw new Error(await parseError(res, editingId ? 'Erro ao atualizar morador.' : 'Erro ao cadastrar morador.'))
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
        Unidade *
        <Select name="unidadeId" value={form.unidadeId} onChange={onChange} required>
          <option value="">Selecione...</option>
          {unidades.map((u) => <option key={u.id} value={u.id}>{u.rotulo}</option>)}
        </Select>
      </label>
      <label>Nome *<Input name="nome" value={form.nome} onChange={onChange} required maxLength={150} /></label>
      <label>
        Papel
        <Select name="papel" value={form.papel} onChange={onChange}>
          {PAPEIS.map((p) => <option key={p} value={p}>{p}</option>)}
        </Select>
      </label>
      <label>Email<Input name="email" type="email" value={form.email} onChange={onChange} maxLength={150} /></label>
      <label>Telefone<Input name="telefone" value={form.telefone} onChange={onChange} maxLength={30} /></label>
      <label className="full">Observações<Textarea name="observacoes" value={form.observacoes} onChange={onChange} rows={2} /></label>
      <div className="notice-box full">
        <strong>Aviso LGPD:</strong> Ao cadastrar dados de moradores, prestadores de serviço ou terceiros, declaro que possuo autorização, obrigação legal, relação contratual ou outra base legal adequada para realizar esse cadastro, responsabilizando-me pela exatidão das informações inseridas e pelo uso da plataforma conforme a LGPD.
      </div>
      <div className="item-actions full">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Cadastrar morador'}
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

export default MoradorForm
