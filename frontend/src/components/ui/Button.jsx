import { forwardRef } from 'react'

const Button = forwardRef(function Button({ variant = 'primary', className = '', type = 'button', ...props }, ref) {
  const variantClass = variant === 'danger' ? 'ui-button--danger' : variant === 'secondary' ? 'ui-button--secondary' : 'ui-button--primary'
  const mergedClassName = `ui-button ${variantClass} ${className}`.trim()

  return <button ref={ref} type={type} className={mergedClassName} {...props} />
})

export default Button
