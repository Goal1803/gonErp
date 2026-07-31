import { Extension } from '@tiptap/core'
import Suggestion from '@tiptap/suggestion'
import SlashMenu from '../SlashMenu.vue'
import { createSuggestionRenderer } from './suggestionRenderer'

/**
 * Every block type reachable by typing "/". `run` receives the TipTap chain
 * with the "/query" text already deleted.
 */
export const SLASH_ITEMS = [
  {
    title: 'Text', group: 'Cơ bản', icon: 'notes', keywords: 'text paragraph van ban',
    run: (chain) => chain.setParagraph().run()
  },
  {
    title: 'Heading 1', group: 'Cơ bản', icon: 'title', keywords: 'h1 heading tieu de',
    run: (chain) => chain.setNode('heading', { level: 1 }).run()
  },
  {
    title: 'Heading 2', group: 'Cơ bản', icon: 'title', keywords: 'h2 heading tieu de',
    run: (chain) => chain.setNode('heading', { level: 2 }).run()
  },
  {
    title: 'Heading 3', group: 'Cơ bản', icon: 'title', keywords: 'h3 heading tieu de',
    run: (chain) => chain.setNode('heading', { level: 3 }).run()
  },
  {
    title: 'Bullet list', group: 'Danh sách', icon: 'format_list_bulleted', keywords: 'ul bullet danh sach',
    run: (chain) => chain.toggleBulletList().run()
  },
  {
    title: 'Numbered list', group: 'Danh sách', icon: 'format_list_numbered', keywords: 'ol ordered so',
    run: (chain) => chain.toggleOrderedList().run()
  },
  {
    title: 'To-do list', group: 'Danh sách', icon: 'check_box', keywords: 'todo task checkbox viec',
    run: (chain) => chain.toggleTaskList().run()
  },
  {
    title: 'Toggle list', group: 'Danh sách', icon: 'expand_more', keywords: 'toggle collapse gap',
    run: (chain) => chain.setToggleBlock().run()
  },
  {
    title: 'Toggle heading 1', group: 'Danh sách', icon: 'expand_more', keywords: 'toggle heading h1 gap tieu de',
    run: (chain) => chain.setToggleHeading(1).run()
  },
  {
    title: 'Toggle heading 2', group: 'Danh sách', icon: 'expand_more', keywords: 'toggle heading h2 gap tieu de',
    run: (chain) => chain.setToggleHeading(2).run()
  },
  {
    title: 'Toggle heading 3', group: 'Danh sách', icon: 'expand_more', keywords: 'toggle heading h3 gap tieu de',
    run: (chain) => chain.setToggleHeading(3).run()
  },
  {
    title: 'Callout', group: 'Khối', icon: 'lightbulb', keywords: 'callout note info luu y',
    run: (chain) => chain.toggleCallout({ emoji: '💡', tone: 'info' }).run()
  },
  {
    title: 'Quote', group: 'Khối', icon: 'format_quote', keywords: 'quote blockquote trich dan',
    run: (chain) => chain.toggleBlockquote().run()
  },
  {
    title: 'Code block', group: 'Khối', icon: 'code', keywords: 'code snippet',
    run: (chain) => chain.toggleCodeBlock().run()
  },
  {
    title: 'Divider', group: 'Khối', icon: 'horizontal_rule', keywords: 'hr divider duong ke',
    run: (chain) => chain.setHorizontalRule().run()
  },
  {
    title: 'Table', group: 'Khối', icon: 'table_chart', keywords: 'table bang grid',
    run: (chain) => chain.insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
  },
  {
    title: 'Image', group: 'Nhúng', icon: 'image', keywords: 'image picture anh hinh',
    run: (chain, editor) => {
      const url = window.prompt('URL ảnh:')
      if (url) chain.setImage({ src: url }).run()
      else editor.commands.focus()
    }
  },
  {
    title: 'Link to note', group: 'Nhúng', icon: 'alternate_email', keywords: 'mention link note lien ket',
    run: (chain) => chain.insertContent('@').run()
  },
  {
    title: 'Trang con', group: 'Nhúng', icon: 'insert_drive_file', keywords: 'page subpage trang con nested',
    // The '/page' text is already gone; NoteEditor's handler creates the child
    // note and inserts the node once the server answers.
    run: (chain, editor) => {
      chain.run()
      editor.storage.subpage?.create?.()
    }
  }
]

export const SlashCommand = Extension.create({
  name: 'slashCommand',

  addOptions () {
    return {
      suggestion: {
        char: '/',
        startOfLine: false,
        command: ({ editor, range, props }) => {
          props.run(editor.chain().focus().deleteRange(range), editor)
        }
      }
    }
  },

  addProseMirrorPlugins () {
    return [
      Suggestion({
        editor: this.editor,
        ...this.options.suggestion,
        items: ({ query }) => {
          const q = query.toLowerCase().trim()
          if (!q) return SLASH_ITEMS
          return SLASH_ITEMS.filter(i =>
            i.title.toLowerCase().includes(q) || i.keywords.includes(q))
        },
        render: createSuggestionRenderer(SlashMenu)
      })
    ]
  }
})

export default SlashCommand
