/**
 * Faz o parse do corpo da resposta como JSON de forma segura.
 * Lança um erro descritivo se o servidor retornar HTML em vez de JSON
 * (ex: redirect para página de login, 404 do Vite/Vercel, etc.).
 *
 * @param {Response} response
 * @returns {Promise<unknown>}
 */
export async function parseJson(response) {
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    throw new Error(
      `Resposta inesperada do servidor (${response.status}). Verifique se o backend está rodando e se a sessão está ativa.`
    )
  }
  return response.json()
}
