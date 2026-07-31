import { Extension } from '@tiptap/core'
import { Plugin, PluginKey, NodeSelection, TextSelection } from '@tiptap/pm/state'

/**
 * Can this node type hold ordinary blocks? `doc`, `toggleContent`, `callout`,
 * `blockquote` and `listItem` can; `toggleBlock` (whose content is fixed to
 * summary + body) cannot, which is what makes hovering a toggle's title grab
 * the whole toggle rather than its inner text.
 */
function holdsBlocks (type, schema) {
  return type.contentMatch.matchType(schema.nodes.paragraph) != null
}

/**
 * Position of the block the gutter should attach to for a document position.
 *
 * Picks the *deepest* block whose parent accepts blocks, so a paragraph nested
 * inside a toggle is draggable on its own instead of dragging the whole toggle.
 * List items are grabbed as a unit — dragging the bare paragraph out of a
 * listItem would leave an empty bullet behind.
 *
 * Exported for tests.
 */
export function resolveBlockTarget (state, pos) {
  const $pos = state.doc.resolve(Math.max(0, Math.min(pos, state.doc.content.size)))
  for (let d = $pos.depth; d >= 1; d--) {
    const parent = $pos.node(d - 1)
    if (!holdsBlocks(parent.type, state.schema)) {
      continue
    }
    if (parent.type.name === 'listItem' && d >= 2) {
      // Prefer the list item itself, and the bullet it belongs to stays intact.
      return $pos.before(d - 1)
    }
    return $pos.before(d)
  }
  return null
}

/** Nearest enclosing collapsed toggle, so a drag can pop it open. */
function closedToggleAt (state, pos) {
  const $pos = state.doc.resolve(Math.max(0, Math.min(pos, state.doc.content.size)))
  for (let d = $pos.depth; d >= 1; d--) {
    const node = $pos.node(d)
    if (node.type.name === 'toggleBlock' && node.attrs.open === false) {
      return { node, pos: $pos.before(d) }
    }
  }
  return null
}

/**
 * The hover gutter Notion has on every block: a "+" that inserts a new block
 * below and a grip that drags the block elsewhere.
 *
 * Dragging works by putting a NodeSelection on the hovered block and handing
 * ProseMirror the resulting slice through `view.dragging`; the editor's own
 * drop handling then does the move, so we never touch the document directly.
 */
export const BlockHandle = Extension.create({
  name: 'blockHandle',

  addOptions () {
    return { onPlusClick: null }
  },

  addProseMirrorPlugins () {
    const options = this.options
    let gutter = null
    let hoveredPos = null
    let dragging = false

    const hide = () => { if (gutter) gutter.style.display = 'none' }

    return [
      new Plugin({
        key: new PluginKey('blockHandle'),

        view: (view) => {
          gutter = document.createElement('div')
          gutter.className = 'block-handle-gutter'
          gutter.style.display = 'none'
          gutter.innerHTML =
            '<button class="bh-btn bh-plus" type="button" title="Thêm block bên dưới">+</button>' +
            '<button class="bh-btn bh-grip" type="button" draggable="true" title="Kéo để di chuyển">⠿</button>'
          view.dom.parentNode.appendChild(gutter)

          const plus = gutter.querySelector('.bh-plus')
          const grip = gutter.querySelector('.bh-grip')

          plus.addEventListener('click', (e) => {
            e.preventDefault()
            if (hoveredPos == null) return
            const node = view.state.doc.nodeAt(hoveredPos)
            if (!node) return
            const insertAt = hoveredPos + node.nodeSize
            const tr = view.state.tr.insert(
              insertAt, view.state.schema.nodes.paragraph.create()
            )
            view.dispatch(tr)
            view.focus()
            // Land the caret in the fresh paragraph, then offer the slash menu.
            view.dispatch(view.state.tr.setSelection(
              TextSelection.near(view.state.doc.resolve(insertAt + 1))))
            options.onPlusClick?.()
          })

          grip.addEventListener('mousedown', (e) => {
            if (hoveredPos == null) return
            e.preventDefault()
            view.dispatch(view.state.tr.setSelection(
              NodeSelection.create(view.state.doc, hoveredPos)))
            view.focus()
          })

          grip.addEventListener('dragstart', (e) => {
            if (hoveredPos == null) return
            dragging = true
            const tr = view.state.tr.setSelection(
              NodeSelection.create(view.state.doc, hoveredPos))
            view.dispatch(tr)
            const slice = view.state.selection.content()
            e.dataTransfer.effectAllowed = 'move'
            e.dataTransfer.setData('text/plain', '')
            const dom = view.nodeDOM(hoveredPos)
            if (dom instanceof HTMLElement) e.dataTransfer.setDragImage(dom, 0, 0)
            view.dragging = { slice, move: true }
            hide()
          })

          grip.addEventListener('dragend', () => {
            dragging = false
            view.dragging = null
          })

          return {
            destroy () {
              gutter?.remove()
              gutter = null
            }
          }
        },

        props: {
          handleDOMEvents: {
            mousemove: (view, event) => {
              if (!view.editable || !gutter || dragging) return false
              const posInfo = view.posAtCoords({ left: event.clientX, top: event.clientY })
              if (!posInfo) { hide(); return false }

              const pos = resolveBlockTarget(
                view.state, posInfo.inside >= 0 ? posInfo.inside : posInfo.pos)
              if (pos == null) { hide(); return false }

              const dom = view.nodeDOM(pos)
              if (!(dom instanceof HTMLElement)) { hide(); return false }

              hoveredPos = pos
              const box = dom.getBoundingClientRect()
              const wrap = view.dom.parentNode.getBoundingClientRect()
              gutter.style.display = 'flex'
              gutter.style.top = `${box.top - wrap.top}px`
              gutter.style.left = `${box.left - wrap.left - 52}px`
              return false
            },

            // Dragging over a collapsed toggle opens it, so its body becomes a
            // drop target instead of a wall.
            dragover: (view, event) => {
              if (!view.editable) return false
              const posInfo = view.posAtCoords({ left: event.clientX, top: event.clientY })
              if (!posInfo) return false
              const found = closedToggleAt(view.state, posInfo.inside >= 0 ? posInfo.inside : posInfo.pos)
              if (!found) return false
              view.dispatch(view.state.tr
                .setNodeMarkup(found.pos, null, { ...found.node.attrs, open: true })
                .setMeta('addToHistory', false))
              return false
            },

            drop: () => { dragging = false; hide(); return false },
            mouseleave: () => { if (!dragging) hide(); return false }
          }
        }
      })
    ]
  }
})

export default BlockHandle
