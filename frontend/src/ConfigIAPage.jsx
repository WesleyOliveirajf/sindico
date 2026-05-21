import { useEffect, useState } from 'react'
import { getIAConfig, saveIAConfig, testIAConfig } from './api'
import { LoadingState, SuccessState } from './components/PageFeedback'

const PROVIDERS = [
  { value: 'OPENAI',        label: 'OpenAI',       modelHint: 'gpt-4o-mini, gpt-4o, gpt-4-turbo' },
  { value: 'ANTHROPIC',     label: 'Anthropic',    modelHint: 'claude-sonnet-4-6, claude-3-haiku' },
  { value: 'GOOGLE_GEMINI', label: 'Google Gemini', modelHint: 'gemini-1.5-flash, gemini-1.5-pro' },
  { value: 'GROQ',          label: 'Groq',         modelHint: 'llama-3.1-70b-versatile, mixtral-8x7b' },
  { value: 'OLLAMA',        label: 'Ollama (local)', modelHint: 'llama3, mistral, codellama' },
]

function ConfigIAPage({ embedded = false }) {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [testing, setTesting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [testResult, setTestResult] = useState(null)

  const [provider, setProvider] = useState('OPENAI')
  const [apiKey, setApiKey] = useState('')
  const [model, setModel] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [ativo, setAtivo] = useState(false)
  const [configurado, setConfigurado] = useState(false)

  useEffect(() => {
    async function load() {
      try {
        const data = await getIAConfig()
        if (data.provider) setProvider(data.provider)
        if (data.model) setModel(data.model)
        if (data.baseUrl) setBaseUrl(data.baseUrl)
        setAtivo(data.ativo)
        setConfigurado(data.configurado)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  async function handleSave(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setTestResult(null)
    setSaving(true)
    try {
      const payload = { provider, model: model || null, baseUrl: baseUrl || null, ativo }
      if (apiKey.trim()) payload.apiKey = apiKey.trim()
      const data = await saveIAConfig(payload)
      setConfigurado(data.configurado)
      setApiKey('')
      setSuccess('Configuracao salva com sucesso.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleTest() {
    setError('')
    setSuccess('')
    setTestResult(null)
    setTesting(true)
    try {
      const data = await testIAConfig()
      setTestResult(data)
    } catch (err) {
      setTestResult({ status: 'erro', message: err.message })
    } finally {
      setTesting(false)
    }
  }

  const providerInfo = PROVIDERS.find((p) => p.value === provider)

  if (loading) {
    return (
      <>
        {!embedded && (
          <section className="hero">
            <p className="eyebrow">Inteligencia Artificial</p>
            <h1>Configuracao de IA</h1>
          </section>
        )}
        <LoadingState message="Carregando configuracao..." />
      </>
    )
  }

  return (
    <>
      {!embedded && (
        <section className="hero">
          <p className="eyebrow">Inteligencia Artificial</p>
          <h1>Configuracao de IA</h1>
          <p className="subtitle">
            Configure seu provedor de IA para habilitar o assistente, geracao de atas, analise de gastos e triagem de manutencoes.
          </p>
        </section>
      )}

      <SuccessState message={success} />

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Provedor e credenciais</h2>

        {error && <p className="message error">{error}</p>}

        <form onSubmit={handleSave} className="form-grid">
          <label>
            Provedor *
            <select value={provider} onChange={(e) => setProvider(e.target.value)}>
              {PROVIDERS.map((p) => (
                <option key={p.value} value={p.value}>{p.label}</option>
              ))}
            </select>
          </label>

          <label>
            Modelo
            <input
              value={model}
              onChange={(e) => setModel(e.target.value)}
              placeholder={providerInfo?.modelHint || ''}
              maxLength={100}
            />
            <span className="muted" style={{ fontSize: '0.78rem' }}>
              Deixe vazio para usar o modelo padrao do provedor.
            </span>
          </label>

          <label className="full">
            Chave de API {configurado ? '(ja configurada — preencha apenas para alterar)' : '*'}
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder={configurado ? '••••••••••••' : 'Cole aqui sua chave de API'}
              autoComplete="off"
            />
          </label>

          {provider === 'OLLAMA' && (
            <label className="full">
              Base URL (Ollama)
              <input
                value={baseUrl}
                onChange={(e) => setBaseUrl(e.target.value)}
                placeholder="http://localhost:11434"
              />
            </label>
          )}

          <label className="full" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: 10 }}>
            <input
              type="checkbox"
              checked={ativo}
              onChange={(e) => setAtivo(e.target.checked)}
              style={{ width: 'auto', marginTop: 0 }}
            />
            <span>IA ativa — habilitar assistente e funcionalidades de IA</span>
          </label>

          <div className="full" style={{ display: 'flex', gap: 10 }}>
            <button type="submit" disabled={saving} className="submit">
              {saving ? 'Salvando...' : 'Salvar configuracao'}
            </button>
            <button
              type="button"
              disabled={testing || !configurado}
              className="submit"
              style={{ background: '#475569' }}
              onClick={handleTest}
            >
              {testing ? 'Testando...' : 'Testar conexao'}
            </button>
          </div>
        </form>

        {testResult && (
          <div
            className={`message ${testResult.status === 'ok' ? 'success' : 'error'}`}
            style={{ marginTop: 14 }}
          >
            {testResult.status === 'ok' ? (
              <>
                <strong>Conexao OK!</strong> Provider: {testResult.provider} · Modelo: {testResult.model}
                {testResult.resposta ? ` · Resposta: "${testResult.resposta}"` : ''}
              </>
            ) : (
              <>
                <strong>Falha na conexao.</strong> {testResult.message}
              </>
            )}
          </div>
        )}
      </section>

      <section className="panel" style={{ marginTop: 20 }}>
        <h2>Como funciona</h2>
        <div style={{ color: 'var(--muted)', lineHeight: 1.65, fontSize: '0.92rem' }}>
          <p style={{ margin: '0 0 10px' }}>
            Ao configurar e ativar a IA, voce habilita as seguintes funcionalidades:
          </p>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li><strong>Assistente de Chat</strong> — pergunte sobre gestao de condominios, legislacao e melhores praticas.</li>
            <li><strong>Gerar Ata</strong> — gere atas formais a partir dos dados de qualquer reuniao registrada.</li>
            <li><strong>Analise de Gastos</strong> — receba analise financeira, alertas e sugestoes de economia.</li>
            <li><strong>Triagem de Manutencao</strong> — descreva um problema e a IA sugere tipo, categoria e urgencia.</li>
          </ul>
          <p style={{ margin: '10px 0 0' }}>
            A chave de API e armazenada de forma criptografada (AES-GCM) no banco de dados.
            Cada condominio tem sua propria configuracao independente.
          </p>
        </div>
      </section>
    </>
  )
}

export default ConfigIAPage
