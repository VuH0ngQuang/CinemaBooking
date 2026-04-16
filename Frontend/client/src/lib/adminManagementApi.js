import { buildApiUrl } from './api'

const fetchJson = async (path, token) => {
  const response = await fetch(buildApiUrl(path), {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || `Failed to fetch ${path}`)
  }

  return JSON.parse(text)
}

export const getAllUsers = async (token) => {
  return fetchJson('/api/users', token)
}

export const getAllCinemas = async (token) => {
  return fetchJson('/api/cinemas', token)
}

export const getAllScreeningRooms = async (token) => {
  return fetchJson('/api/screeningrooms', token)
}

export const getAllBookingSeats = async (token) => {
  return fetchJson('/api/booking-seats', token)
}

export const getAllTickets = async (token) => {
  return fetchJson('/api/tickets', token)
}

export const getAllBookings = async (token) => {
  return fetchJson('/api/bookings', token)
}

export const getAllShowtimesForAdmin = async (token) => {
  return fetchJson('/api/showtimes', token)
}

export const getAllMoviesForAdmin = async (token) => {
  return fetchJson('/api/movies', token)
}