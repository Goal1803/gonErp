import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from 'src/stores/authStore'
import { useNotificationStore } from 'src/stores/notificationStore'

export function useNotificationSocket() {
  const authStore = useAuthStore()
  const notificationStore = useNotificationStore()
  let client = null

  const connect = () => {
    if (!authStore.token) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const brokerURL = `${protocol}//${window.location.host}/api/ws`

    client = new Client({
      brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${authStore.token}`
      },
      reconnectDelay: 4000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const notification = JSON.parse(message.body)
            notificationStore.addRealTimeNotification(notification)
          } catch (e) {
            console.error('[NotificationSocket] parse error', e)
          }
        })
      },
      onStompError: (frame) => {
        console.error('[NotificationSocket] STOMP error', frame)
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
