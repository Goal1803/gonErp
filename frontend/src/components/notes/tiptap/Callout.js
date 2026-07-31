import { Node, mergeAttributes } from '@tiptap/core'
import { Plugin } from '@tiptap/pm/state'

const TONES = ['info', 'success', 'warning', 'danger', 'neutral']

/**
 * Notion-style callout: a tinted box holding normal blocks, with an emoji
 * rendered from an attribute via CSS so no node view is needed.
 * Clicking the emoji cycles the tone.
 */
export const Callout = Node.create({
  name: 'callout',
  group: 'block',
  content: 'block+',
  defining: true,

  addOptions () {
    return { HTMLAttributes: {} }
  },

  addAttributes () {
    return {
      emoji: {
        default: '💡',
        parseHTML: (el) => el.getAttribute('data-emoji') || '💡',
        renderHTML: (attrs) => ({ 'data-emoji': attrs.emoji })
      },
      tone: {
        default: 'info',
        parseHTML: (el) => el.getAttribute('data-tone') || 'info',
        renderHTML: (attrs) => ({ 'data-tone': attrs.tone })
      }
    }
  },

  parseHTML () {
    return [{ tag: 'div[data-type="callout"]' }]
  },

  renderHTML ({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, { 'data-type': 'callout' }),
      ['div', { class: 'callout-emoji', contenteditable: 'false' }],
      ['div', { class: 'callout-body' }, 0]
    ]
  },

  addCommands () {
    return {
      setCallout: (attrs = {}) => ({ commands }) => commands.wrapIn(this.name, attrs),
      toggleCallout: (attrs = {}) => ({ commands }) => commands.toggleWrap(this.name, attrs)
    }
  },

  addProseMirrorPlugins () {
    const type = this.name
    // Must be a real Plugin instance — ProseMirror reads plugin.spec on create.
    return [new Plugin({
      props: {
        handleDOMEvents: {
          mousedown: (view, event) => {
            const emoji = event.target.closest?.('.callout-emoji')
            if (!emoji) return false
            const box = emoji.closest('[data-type="callout"]')
            if (!box) return false
            const pos = view.posAtDOM(box, 0)
            const $pos = view.state.doc.resolve(pos)
            // Walk out to the callout itself — posAtDOM lands inside its content.
            for (let d = $pos.depth; d > 0; d--) {
              if ($pos.node(d).type.name === type) {
                const node = $pos.node(d)
                const next = TONES[(TONES.indexOf(node.attrs.tone) + 1) % TONES.length]
                view.dispatch(view.state.tr.setNodeMarkup($pos.before(d), null, {
                  ...node.attrs, tone: next
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

export default Callout
