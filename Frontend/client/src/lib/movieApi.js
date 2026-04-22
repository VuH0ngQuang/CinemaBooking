import { buildApiUrl } from './api'

export const getAllMoviesForAdmin = async (token) => {
  const response = await fetch(buildApiUrl('/api/movies'), {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Failed to fetch movies')
  }

  return JSON.parse(text)
}

export const createMovie = async (moviePayload, token) => {
  const response = await fetch(buildApiUrl('/api/movies'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(moviePayload),
  })

  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(responseText || 'Create movie failed')
  }

  return JSON.parse(responseText)
}

export const deleteMovie = async (movieId, token) => {
  const response = await fetch(buildApiUrl(`/api/movies/${movieId}`), {
    method: 'DELETE',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Delete movie failed')
  }

  return text
}