function Input({ className = '', ...props }) {
  const mergedClassName = `ui-input ${className}`.trim()
  return <input className={mergedClassName} {...props} />
}

export function Select({ className = '', ...props }) {
  const mergedClassName = `ui-input ${className}`.trim()
  return <select className={mergedClassName} {...props} />
}

export function Textarea({ className = '', ...props }) {
  const mergedClassName = `ui-input ${className}`.trim()
  return <textarea className={mergedClassName} {...props} />
}

export default Input
