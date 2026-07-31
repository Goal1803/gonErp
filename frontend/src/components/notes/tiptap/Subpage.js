import { Node, mergeAttributes } from '@tiptap/core'

/**
 * An inline link to a child page, the way Notion nests pages inside a document.
 *
 * The node only stores a pointer (id + a cached title/icon for rendering); the
 * page itself is a real note whose parent is the note being edited, so it also
 * shows up nested in the sidebar tree.
 *
 * `storage.create` is filled in by NoteEditor — the node cannot create pages by
 * itself because that needs the id of the note currently open.
 */
export const Subpage = Node.create({
  name: 'subpage',
  group: 'block',
  atom: true,
  draggable: true,
  selectable: true,

  addStorage () {
    return {
      /** async () => void — creates a child note and inserts a node for it. */
      create: null
    }
  },

  addAttributes () {
    return {
      id: {
        default: null,
        parseHTML: (el) => el.getAttribute('data-id'),
        renderHTML: (attrs) => ({ 'data-id': attrs.id })
      },
      title: {
        default: 'Untitled',
        parseHTML: (el) => el.getAttribute('data-title') || 'Untitled',
        renderHTML: (attrs) => ({ 'data-title': attrs.title })
      },
      icon: {
        default: '📄',
        parseHTML: (el) => el.getAttribute('data-icon') || '📄',
        renderHTML: (attrs) => ({ 'data-icon': attrs.icon })
      }
    }
  },

  parseHTML () {
    return [{ tag: 'div[data-type="subpage"]' }]
  },

  renderHTML ({ node, HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, { 'data-type': 'subpage', class: 'note-subpage' }),
      ['span', { class: 'note-subpage-icon' }, node.attrs.icon || '📄'],
      ['span', { class: 'note-subpage-title' }, node.attrs.title || 'Untitled']
    ]
  },

  renderText ({ node }) {
    return node.attrs.title || ''
  },

  addCommands () {
    return {
      insertSubpage: (attrs) => ({ chain }) =>
        chain().insertContent({ type: this.name, attrs }).run()
    }
  }
})

export default Subpage
