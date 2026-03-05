export function thumbUrl(url) {
  if (!url) return url
  if (url.includes('_thumb.')) return url
  if (url.startsWith('/api/')) return url + '?thumb=true'  // legacy compat
  const dot = url.lastIndexOf('.')
  return dot === -1 ? url : url.substring(0, dot) + '_thumb.jpg'
}

/**
 * Converts R2 URL to same-origin API proxy path for downloads.
 * e.g. https://cdn.gonerp.com/taskmanager/uuid.ext → /api/tasks/files/uuid.ext
 */
export function downloadUrl(url) {
  if (!url || url.startsWith('/api/')) return url
  try {
    const u = new URL(url)
    const parts = u.pathname.split('/').filter(Boolean) // ['taskmanager', 'uuid.ext']
    if (parts.length >= 2) {
      const prefix = parts[0]
      const filename = parts.slice(1).join('/')
      const map = { images: 'images', taskmanager: 'tasks', users: 'users' }
      if (map[prefix]) return `/api/${map[prefix]}/files/${filename}`
    }
  } catch { /* fall through */ }
  return url
}
