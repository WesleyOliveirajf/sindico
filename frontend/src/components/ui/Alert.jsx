function Alert({ variant = 'info', children, className = '', ...props }) {
  const variantClass = variant === 'error' ? 'ui-alert--error' : variant === 'success' ? 'ui-alert--success' : 'ui-alert--info'
  const mergedClassName = `ui-alert ${variantClass} ${className}`.trim()
  return (
    <p className={mergedClassName} {...props}>
      {children}
    </p>
  )
}

export default Alert
