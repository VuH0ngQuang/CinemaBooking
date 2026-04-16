import { buildApiUrl } from './api'

export const getMovies = async () => {
  const response = await fetch(buildApiUrl('/api/movies'))
  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Failed to fetch movies')
  }

  return JSON.parse(text)
}

export const getScreeningRooms = async () => {
  const response = await fetch(buildApiUrl('/api/screeningrooms'))
  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Failed to fetch screening rooms')
  }

  return JSON.parse(text)
}

export const getAllShowtimes = async () => {
  const response = await fetch(buildApiUrl('/api/showtimes'))
  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Failed to fetch showtimes')
  }

  return JSON.parse(text)
}

export const createShowtime = async (payload, token) => {
  const response = await fetch(buildApiUrl('/api/showtimes'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Create showtime failed')
  }

  return JSON.parse(text)
}

export const deleteShowtime = async (showtimeId, token) => {
  const response = await fetch(buildApiUrl(`/api/showtimes/${showtimeId}`), {
    method: 'DELETE',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Delete showtime failed')
  }

  return text
}