import { useEffect, useRef, useState } from 'react'
import { iaChat } from './api'

const SUGGESTIONS = [
  'Quais sao as obrigacoes legais de um sindico no Brasil?',
  'Como conduzir uma assembleia extraordinaria?',
  'Dicas para reduzir custos de manutencao em condominio.',
  'Como lidar com moradores inadimplentes?',
  'O que deve constar na convencao do condominio?',
]

function AssistenteIAPage() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const scrollRef = useRef(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages])

  async function enviar(textoOpcional) {
    const texto = (textoOpcional || input).trim()
    if (!texto || sending) return

    setInput('')
    const userMsg = { role: 'user', content: texto, ts: Date.now() }
    setMessages((prev) => [...prev, userMsg])
    setSending(true)

    try {
      const data = await iaChat(texto)
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: data.resposta, ts: Date.now() },
      ])
    } catch (err) {
      const isNotConfigured = err.message?.includes('nao configurada') || err.message?.includes('desativada')
      setMessages((prev) => [
        ...prev,
        {
          role: 'error',
          content: isNotConfigured
            ? 'A IA nao esta configurada para este condominio. Va em "Config. IA" no menu lateral para configurar o provedor e a chave de API.'
            : err.message,
          ts: Date.now(),
        },
      ])
    } finally {
      setSending(false)
    }
  }

  function onSubmit(e) {
    e.preventDefault()
    enviar()
  }

  function onKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      enviar()
    }
  }

  const empty = messages.length === 0 && !sending

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Inteligencia Artificial</p>
        <h1>Assistente IA</h1>
        <p className="subtitle">
          Tire duvidas sobre gestao condominial, legislacao, financas e boas praticas.
        </p>
      </section>

      <section className="ia-chat-container" style={{ marginTop: 20 }}>
        <div className="ia-chat-messages" ref={scrollRef}>
          {empty && (
            <div className="ia-chat-empty">
              <p style={{ margin: '0 0 12px', fontWeight: 600 }}>Como posso ajudar?</p>
              <p className="muted" style={{ margin: '0 0 14px', fontSize: '0.88rem' }}>
                Selecione uma sugestao ou escreva sua pergunta:
              </p>
              <div className="ia-suggestions">
                {SUGGESTIONS.map((s, i) => (
                  <button
                    key={i}
                    className="ia-suggestion-btn"
                    onClick={() => enviar(s)}
                    disabled={sending}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg, i) => (
            <div
              key={i}
              className={`ia-chat-bubble ia-chat-bubble--${msg.role}`}
            >
              <div className="ia-chat-bubble-label">
                {msg.role === 'user' ? 'Voce' : msg.role === 'error' ? 'Erro' : 'Assistente'}
              </div>
              <div className="ia-chat-bubble-content">{msg.content}</div>
            </div>
          ))}

          {sending && (
            <div className="ia-chat-bubble ia-chat-bubble--assistant">
              <div className="ia-chat-bubble-label">Assistente</div>
              <div className="ia-chat-thinking">
                <span className="ia-dot"></span>
                <span className="ia-dot"></span>
                <span className="ia-dot"></span>
              </div>
            </div>
          )}
        </div>

        <form className="ia-chat-input-bar" onSubmit={onSubmit}>
          <textarea
            className="ia-chat-input"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder="Digite sua pergunta..."
            rows={1}
            disabled={sending}
          />
          <button type="submit" className="ia-chat-send" disabled={sending || !input.trim()}>
            Enviar
          </button>
        </form>
      </section>
    </>
  )
}

export default AssistenteIAPage
