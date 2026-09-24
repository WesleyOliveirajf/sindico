/**
 * Vercel Edge Middleware — proxy para o backend (VPS) sem CORS.
 *
 * Intercepta /api/* ANTES do rewrite do vercel.json.
 * Remove o header Origin para que o Spring Security não ative
 * o filtro CORS (que rejeitaria origens externas com 403).
 * A autenticação usa Bearer token no header Authorization, portanto
 * não há dependência de cookies cross-site — remoção do Origin é segura.
 */

/* global process */
const BACKEND_URL = 'https://app.analisandoia.com.br'

export default async function middleware(request) {
  const { pathname, search } = new URL(request.url)

  if (!pathname.startsWith('/api/')) return

  // Responde preflight OPTIONS diretamente sem consultar o backend
  if (request.method === 'OPTIONS') {
    return new Response(null, {
      status: 204,
      headers: {
        'Access-Control-Allow-Origin': request.headers.get('origin') || '*',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS, PATCH',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        'Access-Control-Max-Age': '3600',
      },
    })
  }

  const targetUrl = BACKEND_URL + pathname + search

  // Constrói headers sem Origin/Host para não acionar CORS no Spring Security
  const forwardHeaders = new Headers()
  for (const [key, value] of request.headers.entries()) {
    const lower = key.toLowerCase()
    // x-sindico-*: cabecalhos internos; nunca repassar os que vieram do cliente.
    if (lower !== 'origin' && lower !== 'host' && lower !== 'referer' && !lower.startsWith('x-sindico-')) {
      forwardHeaders.set(key, value)
    }
  }

  // O back-end enxerga o IP do Vercel; repassa o IP real do usuario, autenticado por segredo
  // compartilhado (PROXY_SHARED_SECRET no Vercel = APP_TRUSTED_PROXY_SECRET na VPS).
  const proxySecret = process.env.PROXY_SHARED_SECRET
  if (proxySecret) {
    const clientIp = (
      request.headers.get('x-real-ip') ||
      (request.headers.get('x-forwarded-for') || '').split(',')[0]
    ).trim()
    forwardHeaders.set('x-sindico-proxy-secret', proxySecret)
    if (clientIp) forwardHeaders.set('x-sindico-client-ip', clientIp)
  }

  let body = undefined
  if (!['GET', 'HEAD'].includes(request.method)) {
    body = await request.arrayBuffer()
  }

  const upstream = await fetch(targetUrl, {
    method: request.method,
    headers: forwardHeaders,
    body,
  })

  // Repassa a resposta com header CORS para o browser
  const responseHeaders = new Headers(upstream.headers)
  const origin = request.headers.get('origin')
  if (origin) {
    responseHeaders.set('Access-Control-Allow-Origin', origin)
    responseHeaders.set('Access-Control-Allow-Credentials', 'false')
  }
  // Remove cabeçalhos que conflitam com o proxy
  responseHeaders.delete('transfer-encoding')

  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: responseHeaders,
  })
}

export const config = {
  matcher: '/api/:path*',
}
