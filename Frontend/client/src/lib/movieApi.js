import { buildApiUrl } from './api'

export const createMovie = async (moviePayload) => {
  const response = await fetch(buildApiUrl('/api/movies'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(moviePayload),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Create movie failed')
  }

  return JSON.parse(text)
}

export const updateMovie = async (movieId, moviePayload) => {
  const response = await fetch(buildApiUrl(`/api/movies/${movieId}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(moviePayload),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Update movie failed')
  }

  return JSON.parse(text)
}

export const deleteMovie = async (movieId) => {
  const response = await fetch(buildApiUrl(`/api/movies/${movieId}`), {
    method: 'DELETE',
    credentials: 'include',
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || 'Delete movie failed')
  }

  return text
}
