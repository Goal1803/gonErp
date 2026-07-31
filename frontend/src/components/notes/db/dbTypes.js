/**
 * Shared vocabulary for note databases: property types, the "id:label:color"
 * option encoding used by SELECT/MULTI_SELECT, and the filter operators each
 * type supports. Keep the operator ids in step with NoteValueCodec.matches().
 */

export const PROPERTY_TYPES = [
  { value: 'TEXT', label: 'Văn bản', icon: 'notes' },
  { value: 'NUMBER', label: 'Số', icon: 'tag' },
  { value: 'SELECT', label: 'Chọn một', icon: 'radio_button_checked' },
  { value: 'MULTI_SELECT', label: 'Chọn nhiều', icon: 'checklist' },
  { value: 'DATE', label: 'Ngày', icon: 'event' },
  { value: 'CHECKBOX', label: 'Checkbox', icon: 'check_box' },
  { value: 'PERSON', label: 'Người', icon: 'person' },
  { value: 'URL', label: 'Liên kết', icon: 'link' }
]

export const OPTION_COLORS = [
  'grey', 'red', 'orange', 'amber', 'green', 'teal', 'blue', 'purple', 'pink'
]

const OPERATORS = {
  common: [
    { value: 'is_empty', label: 'đang trống', noOperand: true },
    { value: 'is_not_empty', label: 'không trống', noOperand: true }
  ],
  TEXT: [
    { value: 'contains', label: 'chứa' },
    { value: 'not_contains', label: 'không chứa' },
    { value: 'is', label: 'bằng' },
    { value: 'is_not', label: 'khác' }
  ],
  URL: [
    { value: 'contains', label: 'chứa' },
    { value: 'is', label: 'bằng' }
  ],
  NUMBER: [
    { value: 'is', label: '=' },
    { value: 'is_not', label: '≠' },
    { value: 'gt', label: '>' },
    { value: 'gte', label: '≥' },
    { value: 'lt', label: '<' },
    { value: 'lte', label: '≤' }
  ],
  DATE: [
    { value: 'is', label: 'đúng ngày' },
    { value: 'before', label: 'trước ngày' },
    { value: 'after', label: 'sau ngày' }
  ],
  SELECT: [
    { value: 'is', label: 'là' },
    { value: 'is_not', label: 'không là' }
  ],
  MULTI_SELECT: [
    { value: 'is', label: 'có chứa' },
    { value: 'is_not', label: 'không chứa' }
  ],
  PERSON: [
    { value: 'is', label: 'có' },
    { value: 'is_not', label: 'không có' }
  ],
  CHECKBOX: [
    { value: 'checked', label: 'đã tick', noOperand: true },
    { value: 'unchecked', label: 'chưa tick', noOperand: true }
  ]
}

export function operatorsFor (type) {
  const base = OPERATORS[type] || OPERATORS.TEXT
  return type === 'CHECKBOX' ? base : [...base, ...OPERATORS.common]
}

export function typeMeta (type) {
  return PROPERTY_TYPES.find(t => t.value === type) || PROPERTY_TYPES[0]
}

// ── option encoding: "id:label:color" ──

export function parseOption (raw) {
  const [id, label, color] = String(raw).split(':')
  return { id, label: label || id, color: color || 'grey', raw }
}

export function buildOption (id, label, color) {
  // ':' is the separator, so it must not survive inside a field.
  const clean = (s) => String(s ?? '').replace(/:/g, ' ').trim()
  return `${clean(id)}:${clean(label)}:${clean(color) || 'grey'}`
}

export function parseOptions (options) {
  return (options || []).map(parseOption)
}

export function optionLabel (property, id) {
  return parseOptions(property?.options).find(o => o.id === id)?.label || id
}

export function optionColor (property, id) {
  return parseOptions(property?.options).find(o => o.id === id)?.color || 'grey'
}

/** Turns a free-text label into a stable, collision-free option id. */
export function slugify (label, taken = []) {
  const base = String(label).toLowerCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'opt'
  let id = base
  let n = 2
  while (taken.includes(id)) id = `${base}_${n++}`
  return id
}

// ── value helpers ──

export function splitList (value) {
  return value ? String(value).split(',').map(s => s.trim()).filter(Boolean) : []
}

export function formatNumber (value, format) {
  if (value === null || value === undefined || value === '') return ''
  const n = Number(value)
  if (Number.isNaN(n)) return value
  switch (format) {
    case 'INTEGER': return n.toLocaleString('vi-VN', { maximumFractionDigits: 0 })
    case 'MONEY': return n.toLocaleString('vi-VN', { maximumFractionDigits: 2 }) + ' ₫'
    case 'PERCENT': return n.toLocaleString('vi-VN') + '%'
    default: return n.toLocaleString('vi-VN', { maximumFractionDigits: 4 })
  }
}

export function formatDate (value) {
  if (!value) return ''
  const [y, m, d] = String(value).split('-')
  return d ? `${d}/${m}/${y}` : value
}
