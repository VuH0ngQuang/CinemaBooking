import { buildApiUrl } from './api'

export const validateTicketByCode = async (ticketCode) => {
  const response = await fetch(buildApiUrl('/api/tickets/validate'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ ticketCode }),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Ticket validation failed')
  }

  return JSON.parse(text)
}
