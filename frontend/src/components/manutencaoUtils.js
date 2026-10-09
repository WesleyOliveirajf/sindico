export const STATUS_OPTIONS = [
  { value: 'ABERTA', label: 'Aberta' },
  { value: 'AGENDADA', label: 'Agendada' },
  { value: 'EM_ANDAMENTO', label: 'Em andamento' },
  { value: 'CONCLUIDA', label: 'Fechada' },
  { value: 'CANCELADA', label: 'Cancelada' },
]

export function statusLabel(value) {
  return STATUS_OPTIONS.find((status) => status.value === value)?.label || value
}

export function parseCurrencyValue(value) {
  if (value == null || value === '') return null
  if (typeof value === 'number') return Number.isFinite(value) ? value : null

  const sanitized = String(value).trim().replace(/[^\d,.-]/g, '')
  if (!/\d/.test(sanitized)) return null

  const lastComma = sanitized.lastIndexOf(',')
  const lastDot = sanitized.lastIndexOf('.')
  let normalized = sanitized

  if (lastComma > -1 && lastDot > -1) {
    normalized = lastComma > lastDot
      ? sanitized.replace(/\./g, '').replace(',', '.')
      : sanitized.replace(/,/g, '')
  } else if (lastComma > -1) {
    normalized = sanitized.replace(/\./g, '').replace(',', '.')
  } else if (lastDot > -1) {
    const dotCount = (sanitized.match(/\./g) || []).length
    const decimalDigits = sanitized.length - lastDot - 1
    normalized = dotCount === 1 && decimalDigits <= 2
      ? sanitized.replace(/,/g, '')
      : sanitized.replace(/\./g, '')
  }

  const parsed = Number(normalized)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null
}

export function formatCurrency(value) {
  const parsed = parseCurrencyValue(value)
  if (parsed == null) return ''
  return parsed.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}
