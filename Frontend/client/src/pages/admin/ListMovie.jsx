import React, { useEffect, useMemo, useState } from 'react'
import Title from '../../components/admin/Title'
import { useAuth } from '../../context/AuthContext'
import { deleteMovie, getAllMoviesForAdmin } from '../../lib/movieApi'

const formatDate = (value) => {
  if (!value) return 'N/A'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return date.toLocaleDateString()
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return 'N/A'
  return String(value)
}

const ListMovie = () => {
  const { token } = useAuth()

  const [movies, setMovies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionSuccess, setActionSuccess] = useState('')
  const [deletingId, setDeletingId] = useState(null)

  useEffect(() => {
    const fetchMovies = async () => {
      try {
        setLoading(true)
        setError('')

        const data = await getAllMoviesForAdmin(token)
        setMovies(Array.isArray(data) ? data : [])
      } catch (err) {
        setError(err.message || 'Failed to load movies.')
      } finally {
        setLoading(false)
      }
    }

    fetchMovies()
  }, [token])

  const normalizedMovies = useMemo(() => {
    return movies.map((movie, index) => {
      return {
        id: movie.movie_id ?? movie.movieId ?? movie.id ?? index,
        title: movie.title ?? 'N/A',
        genre: movie.genre ?? 'N/A',
        ageRating: movie.age_rating ?? movie.ageRating ?? 'N/A',
        durationMinutes: movie.duration_minutes ?? movie.durationMinutes ?? 'N/A',
        releaseDate: movie.release_date ?? movie.releaseDate ?? '',
        status: movie.status ?? 'N/A',
        trailerUrl: movie.trailerUrl ?? movie.trailer_url ?? '',
        description: movie.description ?? '',
      }
    })
  }, [movies])

  const handleDeleteMovie = async (movieId, movieTitle) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete movie "${movieTitle}" (#${movieId})?`
    )

    if (!confirmed) return

    try {
      setDeletingId(movieId)
      setActionError('')
      setActionSuccess('')

      await deleteMovie(movieId, token)

      setMovies((prev) =>
        prev.filter((movie) => {
          const currentId = movie.movie_id ?? movie.movieId ?? movie.id
          return currentId !== movieId
        })
      )

      setActionSuccess(`Movie "${movieTitle}" deleted successfully.`)
    } catch (err) {
      setActionError(err.message || 'Failed to delete movie.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className='space-y-6'>
      <Title text1='List' text2='Movies' />

      {loading && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 p-4 text-sm text-gray-300'>
          Loading movies...
        </div>
      )}

      {error && (
        <div className='rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300'>
          {error}
        </div>
      )}

      {actionError && (
        <div className='rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300'>
          {actionError}
        </div>
      )}

      {actionSuccess && (
        <div className='rounded-xl border border-green-500/30 bg-green-500/10 p-4 text-sm text-green-300'>
          {actionSuccess}
        </div>
      )}

      {!loading && !error && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 overflow-hidden'>
          <div className='overflow-x-auto'>
            <table className='w-full text-sm'>
              <thead className='bg-black/30 text-left'>
                <tr>
                  <th className='px-4 py-3'>Movie ID</th>
                  <th className='px-4 py-3'>Title</th>
                  <th className='px-4 py-3'>Genre</th>
                  <th className='px-4 py-3'>Age Rating</th>
                  <th className='px-4 py-3'>Duration</th>
                  <th className='px-4 py-3'>Release Date</th>
                  <th className='px-4 py-3'>Status</th>
                  <th className='px-4 py-3'>Trailer</th>
                  <th className='px-4 py-3'>Actions</th>
                </tr>
              </thead>

              <tbody>
                {normalizedMovies.length === 0 ? (
                  <tr>
                    <td colSpan='9' className='px-4 py-6 text-center text-gray-400'>
                      No movies found.
                    </td>
                  </tr>
                ) : (
                  normalizedMovies.map((movie) => (
                    <tr
                      key={movie.id}
                      className='border-t border-white/10 hover:bg-white/5 transition-colors'
                    >
                      <td className='px-4 py-3'>{movie.id}</td>
                      <td className='px-4 py-3'>{formatValue(movie.title)}</td>
                      <td className='px-4 py-3'>{formatValue(movie.genre)}</td>
                      <td className='px-4 py-3'>{formatValue(movie.ageRating)}</td>
                      <td className='px-4 py-3'>
                        {movie.durationMinutes !== 'N/A'
                          ? `${movie.durationMinutes} min`
                          : 'N/A'}
                      </td>
                      <td className='px-4 py-3'>{formatDate(movie.releaseDate)}</td>
                      <td className='px-4 py-3'>{formatValue(movie.status)}</td>
                      <td className='px-4 py-3'>
                        {movie.trailerUrl ? (
                          <a
                            href={movie.trailerUrl}
                            target='_blank'
                            rel='noreferrer'
                            className='text-primary underline'
                          >
                            Open
                          </a>
                        ) : (
                          'N/A'
                        )}
                      </td>
                      <td className='px-4 py-3'>
                        <button
                          onClick={() => handleDeleteMovie(movie.id, movie.title)}
                          disabled={deletingId === movie.id}
                          className='rounded-md bg-red-600 px-3 py-1.5 text-white hover:bg-red-700 disabled:opacity-60 disabled:cursor-not-allowed'
                        >
                          {deletingId === movie.id ? 'Deleting...' : 'Delete'}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

export default ListMovie