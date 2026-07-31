import Mention from '@tiptap/extension-mention'
import { mergeAttributes } from '@tiptap/core'
import MentionList from '../MentionList.vue'
import { createSuggestionRenderer } from './suggestionRenderer'
import { noteApi } from 'src/api/notes'

/**
 * "@" links one note to another. The node name must stay `noteMention` — the
 * backend reads it out of the document JSON to rebuild backlinks.
 */
export const NoteMention = Mention.extend({
  name: 'noteMention',

  renderHTML ({ node, HTMLAttributes }) {
    return [
      'a',
      mergeAttributes(
        { 'data-type': 'noteMention', class: 'note-mention', href: `#/notes/${node.attrs.id}` },
        HTMLAttributes
      ),
      `${node.attrs.label || 'Untitled'}`
    ]
  },

  renderText ({ node }) {
    return node.attrs.label || ''
  }
}).configure({
  suggestion: {
    char: '@',
    allowSpaces: true,
    items: async ({ query }) => {
      try {
        const res = await noteApi.mentions(query, 8)
        return res.data.data || []
      } catch (e) {
        console.error('Mention lookup failed', e)
        return []
      }
    },
    command: ({ editor, range, props }) => {
      editor.chain().focus().insertContentAt(range, [
        { type: 'noteMention', attrs: { id: String(props.id), label: props.title } },
        { type: 'text', text: ' ' }
      ]).run()
    },
    render: createSuggestionRenderer(MentionList)
  }
})

export default NoteMention
