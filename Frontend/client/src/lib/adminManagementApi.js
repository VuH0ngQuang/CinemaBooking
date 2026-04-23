import { buildApiUrl } from './api'

const fetchJson = async (path) => {
  const response = await fetch(buildApiUrl(path), {
    method: 'GET',
    credentials: 'include',
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || `Failed to fetch ${path}`)
  }

  return JSON.parse(text)
}

export const getAllUsers = async () => fetchJson('/api/users')
export const getAllCinemas = async () => fetchJson('/api/cinemas')
export const getAllScreeningRooms = async () => fetchJson('/api/screeningrooms')
export const getAllBookingSeats = async () => fetchJson('/api/booking-seats')
export const getAllTickets = async () => fetchJson('/api/tickets')
export const getAllBookings = async () => fetchJson('/api/bookings')
export const getAllShowtimesForAdmin = async () => fetchJson('/api/showtimes')
export const getAllMoviesForAdmin = async () => fetchJson('/api/movies')
