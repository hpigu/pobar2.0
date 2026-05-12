import { onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * 統一 WebSocket / STOMP client。
 * - 有 JWT (localStorage.token) 自動帶 Authorization header（staff 用）
 * - 沒有 JWT 也能連，後端會依 topic 決定是否放行（顧客 /topic/table/{token}/* OK）
 */
export function useWebSocket(topic, onMessage) {
  const baseUrl = import.meta.env.VITE_API_URL || ''
  let client = null

  function connect() {
    const token = localStorage.getItem('token')
    const connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}

    client = new Client({
      webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
      connectHeaders,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topic, msg => {
          try { onMessage(JSON.parse(msg.body)) } catch (_) {}
        })
      },
      onStompError: frame => {
        // 後端拒絕時 frame.headers.message 會有原因
        console.warn('[WS] STOMP error:', frame.headers?.message)
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
