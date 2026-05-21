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
      `Resposta inesperada do servidor (${response.status}). Verifique se o backend está rodando e se o login foi realizado.`
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
 * Verifica se o usuário está autenticado.
 * Retorna os dados do usuário ou null se não autenticado.
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
 * Autentica o usuário com email e senha.
 * Em caso de sucesso retorna os dados do usuário; em caso de falha lança um erro.
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
    throw new Error(data?.error || 'Credenciais inválidas')
  }
  if (data?.token) {
    setToken(data.token)
  }
  return data
}

/**
 * Cadastra um novo síndico com seu condomínio.
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
    throw new Error(data?.error || 'Não foi possível concluir o cadastro')
  }
  return data
}

/**
 * Encerra a sessão do usuário.
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
 * Retorna métricas gerais da aplicação (requer ROLE_ADMIN).
 */
export async function getAdminStats() {
  const res = await apiFetch('/api/admin/stats')
  if (!res.ok) throw new Error('Não foi possível carregar as estatísticas')
  return parseJson(res)
}

/**
 * Lista todos os usuários cadastrados (requer ROLE_ADMIN).
 * @returns {Promise<Array>}
 */
export async function getAdminUsuarios() {
  const res = await apiFetch('/api/admin/usuarios')
  if (!res.ok) throw new Error('Não foi possível carregar os usuários')
  return parseJson(res)
}

/**
 * Aprova um usuário pendente (requer ROLE_ADMIN).
 * @param {string} id
 */
export async function aprovarUsuario(id) {
  const res = await apiFetch(`/api/admin/usuarios/${id}/aprovar`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Não foi possível aprovar o usuário')
  return data
}

/**
 * Rejeita / desativa um usuário (requer ROLE_ADMIN).
 * @param {string} id
 */
export async function rejeitarUsuario(id) {
  const res = await apiFetch(`/api/admin/usuarios/${id}/rejeitar`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Não foi possível rejeitar o usuário')
  return data
}

/**
 * Reativa um usuário inativo (requer ROLE_ADMIN).
 * @param {string} id
 */
export async function reativarUsuario(id) {
  const res = await apiFetch(`/api/admin/usuarios/${id}/reativar`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Não foi possível reativar o usuário')
  return data
}

// ---------------------------------------------------------------------------
// IA / Assistente
// ---------------------------------------------------------------------------

/**
 * Obtém a configuração de IA do condomínio atual.
 */
export async function getIAConfig() {
  const res = await apiFetch('/api/ia/config')
  if (!res.ok) throw new Error('Não foi possível carregar a configuração de IA')
  return parseJson(res)
}

/**
 * Salva ou atualiza a configuração de IA.
 * @param {{ provider: string, apiKey?: string, model?: string, baseUrl?: string, ativo: boolean }} payload
 */
export async function saveIAConfig(payload) {
  const res = await apiFetch('/api/ia/config', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Erro ao salvar configuração de IA')
  return data
}

/**
 * Testa a conexão com o LLM configurado.
 */
export async function testIAConfig() {
  const res = await apiFetch('/api/ia/config/testar', { method: 'POST' })
  return parseJson(res)
}

/**
 * Envia uma mensagem ao assistente de IA.
 * @param {string} mensagem
 * @returns {Promise<{ resposta: string }>}
 */
export async function iaChat(mensagem) {
  const res = await apiFetch('/api/ia/chat', {
    method: 'POST',
    body: JSON.stringify({ mensagem }),
  })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Erro ao comunicar com o assistente')
  return data
}

/**
 * Gera ata formal de uma reunião via IA.
 * @param {string} reuniaoId
 * @returns {Promise<{ ata: string }>}
 */
export async function iaGerarAta(reuniaoId) {
  const res = await apiFetch(`/api/ia/reuniao/${reuniaoId}/ata`, { method: 'POST' })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Erro ao gerar ata com IA')
  return data
}

/**
 * Solicita análise financeira dos gastos via IA.
 * @param {{ mes?: number, ano?: number }} filtros
 * @returns {Promise<{ analise: string }>}
 */
export async function iaAnalisarGastos(filtros = {}) {
  const params = new URLSearchParams()
  if (filtros.mes) params.set('mes', filtros.mes)
  if (filtros.ano) params.set('ano', filtros.ano)
  const qs = params.toString()
  const path = qs ? `/api/ia/gastos/analise?${qs}` : '/api/ia/gastos/analise'
  const res = await apiFetch(path)
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Erro ao analisar gastos com IA')
  return data
}

/**
 * Triagem inteligente de manutenção via IA.
 * @param {string} descricao
 * @returns {Promise<{ tipo: string, categoria: string, urgencia: string, tituloSugerido: string, observacoes: string }>}
 */
export async function iaTriarManutencao(descricao) {
  const res = await apiFetch('/api/ia/manutencao/triar', {
    method: 'POST',
    body: JSON.stringify({ descricao }),
  })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || 'Erro na triagem de manutenção')
  return data
}
