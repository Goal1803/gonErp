import { Node, mergeAttributes } from '@tiptap/core'
import { Plugin, TextSelection } from '@tiptap/pm/state'

/**
 * Collapsible "toggle list": a summary line plus hidden body blocks.
 *
 * Open/closed is an attribute rendered onto the DOM, and the body is hidden
 * with CSS — that keeps the whole thing a plain node (no Vue node view) while
 * still persisting its state with the document.
 */
export const ToggleSummary = Node.create({
  name: 'toggleSummary',
  content: 'inline*',
  defining: true,
  selectable: false,
  parseHTML () {
    return [{ tag: 'div[data-type="toggleSummary"]' }]
  },
  renderHTML ({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'toggleSummary' }), 0]
  }
})

export const ToggleContent = Node.create({
  name: 'toggleContent',
  content: 'block+',
  defining: true,
  selectable: false,
  parseHTML () {
    return [{ tag: 'div[data-type="toggleContent"]' }]
  },
  renderHTML ({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'toggleContent' }), 0]
  }
})

export const ToggleBlock = Node.create({
  name: 'toggleBlock',
  group: 'block',
  content: 'toggleSummary toggleContent',
  defining: true,

  // Above StarterKit (100) so our Enter/Backspace handling runs before the
  // generic splitBlock/joinBackward keymap claims the key.
  priority: 200,

  addAttributes () {
    return {
      open: {
        default: true,
        parseHTML: (el) => el.getAttribute('data-open') !== 'false',
        renderHTML: (attrs) => ({ 'data-open': attrs.open ? 'true' : 'false' })
      },
      /** 0 = plain toggle, 1..3 = collapsible heading (Notion's toggle headings). */
      level: {
        default: 0,
        parseHTML: (el) => Number(el.getAttribute('data-level')) || 0,
        renderHTML: (attrs) => ({ 'data-level': String(attrs.level || 0) })
      }
    }
  },

  parseHTML () {
    return [{ tag: 'div[data-type="toggleBlock"]' }]
  },

  renderHTML ({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, { 'data-type': 'toggleBlock' }),
      ['div', { class: 'toggle-caret', contenteditable: 'false' }],
      ['div', { class: 'toggle-inner' }, 0]
    ]
  },

  addCommands () {
    return {
      /**
       * Inserts a toggle and leaves the caret in its summary, so whatever the
       * user types next becomes the toggle's title.
       */
      setToggleBlock: (attrs = {}) => ({ state, tr, dispatch, editor }) => {
        const node = editor.schema.nodes[this.name].createAndFill({
          open: true, level: attrs.level || 0
        })
        if (!node) {
          return false
        }
        if (!dispatch) {
          return true
        }
        const { $from } = state.selection
        const depth = $from.depth
        const parent = $from.parent
        let pos
        // Replace the block we are standing in when it is an empty paragraph
        // (the usual case right after typing "/toggle"), otherwise insert after it.
        if (parent.type.name === 'paragraph' && parent.content.size === 0 && depth > 0) {
          pos = $from.before(depth)
          tr.replaceWith(pos, pos + parent.nodeSize, node)
        } else {
          pos = depth > 0 ? $from.after(depth) : state.doc.content.size
          tr.insert(pos, node)
        }
        // toggleBlock at pos → summary opens at pos+1 → its text starts at pos+2.
        tr.setSelection(TextSelection.create(tr.doc, pos + 2))
        tr.scrollIntoView()
        return true
      },

      setToggleHeading: (level) => ({ commands }) => commands.setToggleBlock({ level }),

      toggleFold: () => ({ state, tr, dispatch }) => {
        const info = findToggle(state.selection.$from)
        if (!info) {
          return false
        }
        if (dispatch) {
          tr.setNodeMarkup(info.pos, null, { ...info.node.attrs, open: !info.node.attrs.open })
        }
        return true
      }
    }
  },

  addKeyboardShortcuts () {
    return {
      Enter: ({ editor }) => {
        const { $from, empty } = editor.state.selection
        if (!empty) {
          return false
        }
        const name = $from.parent.type.name

        // In the summary: drop into the body instead of splitting the title.
        if (name === 'toggleSummary') {
          const afterSummary = $from.after($from.depth)
          // toggleContent starts here; its first block's text starts two in.
          const target = afterSummary + 2
          if (target <= editor.state.doc.content.size) {
            return editor.chain().focus().setTextSelection(target).run()
          }
          return false
        }

        // On an empty trailing paragraph inside the body: step out of the toggle,
        // which is how people leave a toggle without reaching for the mouse.
        if (name === 'paragraph' && $from.parent.content.size === 0) {
          const info = findToggle($from)
          if (!info) {
            return false
          }
          const body = $from.node($from.depth - 1)
          const isLast = body.type.name === 'toggleContent' &&
            body.lastChild === $from.parent
          if (!isLast) {
            return false
          }
          return editor.chain()
            .focus()
            .deleteRange({ from: $from.before($from.depth), to: $from.after($from.depth) })
            .insertContentAt(info.pos + info.node.nodeSize - 1, { type: 'paragraph' })
            .run()
        }
        return false
      },

      // Backspace on an empty summary unwraps the toggle back to plain blocks.
      Backspace: ({ editor }) => {
        const { $from, empty } = editor.state.selection
        if (!empty || $from.parent.type.name !== 'toggleSummary' ||
            $from.parent.content.size !== 0 || $from.parentOffset !== 0) {
          return false
        }
        const info = findToggle($from)
        if (!info) {
          return false
        }
        const body = info.node.lastChild
        return editor.chain()
          .focus()
          .insertContentAt(
            { from: info.pos, to: info.pos + info.node.nodeSize },
            body ? body.toJSON().content : { type: 'paragraph' }
          )
          .run()
      },

      'Mod-Enter': ({ editor }) => editor.commands.toggleFold()
    }
  },

  addProseMirrorPlugins () {
    const type = this.name
    // Must be a real Plugin instance — ProseMirror reads plugin.spec on create.
    return [new Plugin({
      props: {
        handleDOMEvents: {
          mousedown: (view, event) => {
            const caret = event.target.closest?.('.toggle-caret')
            if (!caret) return false
            const box = caret.closest('[data-type="toggleBlock"]')
            if (!box) return false
            const pos = view.posAtDOM(box, 0)
            const $pos = view.state.doc.resolve(pos)
            for (let d = $pos.depth; d >= 0; d--) {
              if ($pos.node(d).type.name === type) {
                const node = $pos.node(d)
                view.dispatch(view.state.tr.setNodeMarkup($pos.before(d), null, {
                  ...node.attrs, open: !node.attrs.open
                }))
                event.preventDefault()
                return true
              }
            }
            return false
          }
        }
      }
    })]
  }
})

/** Nearest enclosing toggleBlock for a resolved position. */
function findToggle ($pos) {
  for (let d = $pos.depth; d > 0; d--) {
    const node = $pos.node(d)
    if (node.type.name === 'toggleBlock') {
      return { node, pos: $pos.before(d) }
    }
  }
  return null
}

export default ToggleBlock
