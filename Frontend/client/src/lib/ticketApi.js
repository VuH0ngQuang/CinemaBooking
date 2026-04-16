import { buildApiUrl } from './api'

export const validateTicketByCode = async (ticketCode, token) => {
  const response = await fetch(buildApiUrl('/api/tickets/validate'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ ticketCode }),
  })

  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(responseText || 'Ticket validation failed')
  }

  return JSON.parse(responseText)
}