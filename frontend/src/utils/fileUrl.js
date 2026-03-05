export function thumbUrl(url) {
  if (!url) return url
  if (url.includes('_thumb.')) return url
  if (url.startsWith('/api/')) return url + '?thumb=true'  // legacy compat
  const dot = url.lastIndexOf('.')
  return dot === -1 ? url : url.substring(0, dot) + '_thumb.jpg'
}
