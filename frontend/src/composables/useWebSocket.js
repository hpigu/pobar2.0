import { onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket(topic, onMessage) {
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
  let client = null

  function connect() {
    client = new Client({
      webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topic, msg => {
          try { onMessage(JSON.parse(msg.body)) } catch (_) {}
        })
      },
    })
    client.activate()
  }

  function disconnect() {
    client?.deactivate()
  }

  onUnmounted(disconnect)

  return { connect, disconnect }
}
