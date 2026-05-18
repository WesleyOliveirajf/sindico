function Modal({ open, title, children, onClose }) {
  if (!open) return null

  return (
    <div className="ui-modal-overlay" role="presentation" onClick={onClose}>
      <div className="ui-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        {title ? <h3 className="ui-modal-title">{title}</h3> : null}
        {children}
      </div>
    </div>
  )
}

export default Modal
