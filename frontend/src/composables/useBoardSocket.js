import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from 'src/stores/authStore'

/**
 * Connects to the board's WebSocket topic and calls onEvent for every message.
 * Automatically reconnects on drop. Disconnects on component unmount.
 *
 * @param {() => number} getBoardId  function returning the current board id
 * @param {(event: object) => void} onEvent  callback for each board event
 */
export function useBoardSocket(getBoardId, onEvent, onConnected) {
  const authStore = useAuthStore()
  let client = null

  const connect = () => {
    const boardId = getBoardId()
    if (!boardId) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const brokerURL = `${protocol}//${window.location.host}/api/ws`

    client = new Client({
      brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${authStore.token}`
      },
      reconnectDelay: 4000,
      onConnect: () => {
        if (onConnected) onConnected()
        client.subscribe(`/topic/board/${boardId}`, (message) => {
          try {
            onEvent(JSON.parse(message.body))
          } catch (e) {
            console.error('[BoardSocket] parse error', e)
          }
        })
      },
      onStompError: (frame) => {
        console.error('[BoardSocket] STOMP error', frame)
      }
    })

    client.activate()
  }

  const disconnect = () => {
    if (client) {
      client.deactivate()
      client = null
    }
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return { connect, disconnect }
}
