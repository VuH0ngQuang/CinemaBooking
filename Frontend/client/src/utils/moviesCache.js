export const MOVIES_CACHE_UPDATED_EVENT = 'movies-cache-updated'

export const readMoviesFromCache = () => {
  try {
    const savedMovies = localStorage.getItem('movies')
    if (!savedMovies) return []

    const parsedMovies = JSON.parse(savedMovies)
    return Array.isArray(parsedMovies) ? parsedMovies : []
  } catch (error) {
    console.error('Failed to parse movies from localStorage:', error)
    return []
  }
}

export const notifyMoviesCacheUpdated = () => {
  window.dispatchEvent(new Event(MOVIES_CACHE_UPDATED_EVENT))
}
