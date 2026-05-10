const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

/**
 * Wrapper em torno de fetch que sempre inclui credenciais (cookie de sessao)
 * e o base URL configurado. Todas as chamadas de API devem usar esta funcao.
 *
 * @param {string} path - Caminho relativo ao /api (ex: '/api/compromissos')
 * @param {RequestInit} [options] - Opcoes adicionais do fetch
 * @returns {Promise<Response>}
 */
export function apiFetch(path, options = {}) {
  return fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })
}

/**
 * Faz o parse do corpo da resposta como JSON de forma segura.
 * Lanca um erro descritivo se o servidor retornar HTML em vez de JSON
 * (ex: redirect para pagina de login, 404 do Vite/Vercel, etc.).
 *
 * @param {Response} response
 * @returns {Promise<unknown>}
 */
export async function parseJson(response) {
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    throw new Error(
      `Resposta inesperada do servidor (${response.status}). Verifique se o backend esta rodando e se a sessao esta ativa.`
    )
  }
  return response.json()
}

/**
 * Verifica se o usuario esta autenticado.
 * Retorna os dados do usuario ou null se nao autenticado.
 *
 * @returns {Promise<{email: string, condominioId: string} | null>}
 */
export async function getMe() {
  try {
    const res = await apiFetch('/api/auth/me')
    if (!res.ok) return null
    return res.json()
  } catch {
    return null
  }
}

/**
 * Autentica o usuario com email e senha.
 * Em caso de sucesso retorna os dados do usuario; em caso de falha lanca um erro.
 *
 * @param {string} email
 * @param {string} senha
 * @returns {Promise<{email: string, condominioId: string}>}
 */
export async function login(email, senha) {
  const res = await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, senha }),
  })
  const data = await res.json()
  if (!res.ok) {
    throw new Error(data?.error || 'Credenciais invalidas')
  }
  return data
}

/**
 * Cadastra um novo sindico com seu condominio.
 *
 * @param {{nome: string, email: string, nomeCondominio: string, senha: string, confirmarSenha: string}} payload
 */
export async function register(payload) {
  const res = await apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  const data = await res.json()
  if (!res.ok) {
    throw new Error(data?.error || 'Nao foi possivel concluir o cadastro')
  }
  return data
}

/**
 * Encerra a sessao do usuario.
 */
export async function logout() {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' })
  } catch {
    // Ignora erros de rede no logout
  }
}
