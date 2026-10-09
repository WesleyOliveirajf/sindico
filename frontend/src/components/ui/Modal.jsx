function Modal({ open, title, children, onClose, className = '' }) {
  if (!open) return null

  const mergedClassName = `ui-modal ${className}`.trim()

  return (
    <div className="ui-modal-overlay" role="presentation" onClick={onClose}>
      <div className={mergedClassName} role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        {title ? <h3 className="ui-modal-title">{title}</h3> : null}
        {children}
      </div>
    </div>
  )
}

export default Modal
