function Card({ as: Component = 'section', className = '', ...props }) {
  const mergedClassName = `ui-card ${className}`.trim()
  return <Component className={mergedClassName} {...props} />
}

export default Card
