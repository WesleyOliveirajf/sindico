/** Valor para input[type=date] a partir do JSON da API (string ISO ou array [y, m, d]). */
export function formatDateIso(value) {
  if (value == null) return ''
  if (typeof value === 'string') return value.length >= 10 ? value.slice(0, 10) : value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, m, d] = value
    return `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return ''
}

/** Valor para input[type=datetime-local] no fuso local do navegador. */
export function formatDateTimeLocal(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
