import Alert from './ui/Alert'
import Button from './ui/Button'

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state-block state-block--error" role="alert">
      <p className="state-title">Nao foi possivel carregar os dados.</p>
      <p className="state-text">{message}</p>
      {onRetry ? (
        <Button onClick={onRetry}>
          Tentar novamente
        </Button>
      ) : null}
    </div>
  )
}

export function EmptyState({ message }) {
  return (
    <div className="state-block">
      <p className="state-title">Sem resultados</p>
      <p className="state-text">{message}</p>
    </div>
  )
}

export function LoadingState({ message = 'Carregando dados...' }) {
  return (
    <div className="state-block state-block--loading" aria-busy="true" aria-live="polite">
      <p className="state-text">{message}</p>
    </div>
  )
}

export function SuccessState({ message }) {
  if (!message) return null
  return (
    <Alert variant="success" role="status" aria-live="polite">
      {message}
    </Alert>
  )
}
