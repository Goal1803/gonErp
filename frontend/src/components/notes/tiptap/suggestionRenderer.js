import { VueRenderer } from '@tiptap/vue-3'
import tippy from 'tippy.js'

/**
 * Shared plumbing for TipTap suggestion popups (slash menu, @ mentions):
 * mounts `component` in a tippy bubble anchored to the caret and forwards
 * arrow/enter keys to it.
 */
export function createSuggestionRenderer (component) {
  return () => {
    let renderer
    let popup

    return {
      onStart: (props) => {
        renderer = new VueRenderer(component, { props, editor: props.editor })
        if (!props.clientRect) return
        popup = tippy('body', {
          getReferenceClientRect: props.clientRect,
          appendTo: () => document.body,
          content: renderer.element,
          showOnCreate: true,
          interactive: true,
          trigger: 'manual',
          placement: 'bottom-start',
          maxWidth: 'none'
        })
      },

      onUpdate: (props) => {
        renderer?.updateProps(props)
        if (props.clientRect) {
          popup?.[0]?.setProps({ getReferenceClientRect: props.clientRect })
        }
      },

      onKeyDown: (props) => {
        if (props.event.key === 'Escape') {
          popup?.[0]?.hide()
          return true
        }
        return renderer?.ref?.onKeyDown?.(props) ?? false
      },

      onExit: () => {
        popup?.[0]?.destroy()
        renderer?.destroy()
        popup = null
        renderer = null
      }
    }
  }
}
