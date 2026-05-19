const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'authToken'
const AUTH_CHECK_TIMEOUT_MS = 5000
export const AUTH_EXPIRED_EVENT = 'auth:expired'
const AUTH_MODE = import.meta.env.VITE_AUTH_MODE || 'jwt' // jwt | cookie | hybrid

function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

function setToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token)
}

function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}

function shouldSendAuthHeader() {
  return AUTH_MODE === 'jwt' || AUTH_MODE === 'hybrid'
}

function shouldSendCredentials() {
  return AUTH_MODE === 'cookie' || AUTH_MODE === 'hybrid'
}

/**
 * Wrapper em torno de fetch que envia Bearer token (quando existir)
 * e o base URL configurado. Todas as chamadas de API devem usar esta funcao.
 *
 * @param {string} path - Caminho relativo ao /api (ex: '/api/compromissos')
 * @param {RequestInit} [options] - Opcoes adicionais do fetch
 * @returns {Promise<Response>}
 */
export function apiFetch(path, options = {}) {
  const token = getToken()
  const useAuthHeader = shouldSendAuthHeader()
  const useCredentials = shouldSendCredentials()
  const headers = {
    ...(useAuthHeader && token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  }

  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  return fetch(`${API_BASE}${path}`, {
    ...options,
    ...(useCredentials ? { credentials: 'include' } : {}),
    headers,
  }).then((response) => {
    if (response.status === 401 && path !== '/api/auth/login') {
      clearToken()
      window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT))
    }
    return response
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
      `Resposta inesperada do servidor (${response.status}). Verifique se o backend esta rodando e se o login foi realizado.`
    )
  }
  return response.json()
}

export async function parseError(response, fallbackMessage) {
  try {
    const data = await parseJson(response)
    return data?.message || data?.error || fallbackMessage
  } catch {
    return fallbackMessage
  }
}

/**
 * Verifica se o usuario esta autenticado.
 * Retorna os dados do usuario ou null se nao autenticado.
 *
 * @returns {Promise<{email: string, condominioId: string} | null>}
 */
export async function getMe() {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), AUTH_CHECK_TIMEOUT_MS)
  try {
    const res = await apiFetch('/api/auth/me', { signal: controller.signal })
    if (!res.ok) return null
    return await parseJson(res)
  } catch {
    return null
  } finally {
    clearTimeout(timeoutId)
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
  const data = await parseJson(res)
  if (!res.ok) {
    throw new Error(data?.error || 'Credenciais invalidas')
  }
  if (data?.token) {
    setToken(data.token)
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
  const data = await parseJson(res)
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
  clearToken()
}

// ---------------------------------------------------------------------------
// Admin
// ---------------------------------------------------------------------------

/**
 * Retorna metricas gerais da aplicacao (requer ROLE_ADMIN).
 */
export async function getAdminStats() {
  const res = await apiFetch('/api/admin/stats')
  if (!res.ok) throw new Error('Nao foi possivel carregar as estatisticas')
  return parseJson(res)
}

/**
 * Lista todos os usuarios cadastrados (requer ROLE_ADMIN).
 * @returns {Promise<Array>}
 */
export async function getAdminUsuarios() {
  const res = await apiFetch('/api/admin/usuarios')
  if (!res.ok) throw new Error('Nao foi possivel carregar os usuarios')
  return parseJson(res)
}

/**
 * Aprova um usuario pendente (requer ROLE_ADMIN).
 * @param {string} id
 */
export async function aprovarUsuario(id) {
  const res = await apiFetch(`/api/admin/usuarios/${id}/aprovar`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Nao foi possivel aprovar o usuario')
  return data
}

/**
 * Rejeita / desativa um usuario (requer ROLE_ADMIN).
 * @param {string} id
 */
export async function rejeitarUsuario(id) {
  const res = await apiFetch(`/api/admin/usuarios/${id}/rejeitar`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Nao foi possivel rejeitar o usuario')
  return data
}

/**
 * Reativa um usuario inativo (requer ROLE_ADMIN).
 * @param {string} id
 */
export async function reativarUsuario(id) {
  const res = await apiFetch(`/api/admin/usuarios/${id}/reativar`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Nao foi possivel reativar o usuario')
  return data
}
