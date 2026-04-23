import React, { useEffect, useMemo, useState } from 'react'
import Title from '../../components/admin/Title'
import { deleteMovie, updateMovie } from '../../lib/movieApi'
import { getAllMoviesForAdmin } from '../../lib/adminManagementApi'

const ageRatingOptions = ['G', 'PG', 'PG13', 'R', 'NC17']
const movieStatusOptions = ['COMING_SOON', 'NOW_SHOWING', 'ENDED']
const movieGenreOptions = [
  'ACTION', 'ADVENTURE', 'ANIMATION', 'COMEDY', 'CRIME', 'DRAMA',
  'FANTASY', 'HORROR', 'MYSTERY', 'ROMANCE', 'SCI_FI', 'THRILLER',
]

const formatDate = (value) => {
  if (!value) return 'N/A'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString()
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return 'N/A'
  return String(value)
}

const inputClass = 'w-full rounded-md bg-black/30 border border-white/15 px-3 py-2 outline-none text-sm'

const EditMovieModal = ({ movie, onClose, onSaved }) => {
  const [form, setForm] = useState({
    title: movie.title || '',
    description: movie.description || '',
    age_rating: movie.ageRating || '',
    release_date: movie.releaseDate ? new Date(movie.releaseDate).toISOString().split('T')[0] : '',
    status: movie.status || '',
    genre: movie.genre || '',
    duration_minutes: movie.durationMinutes !== 'N/A' ? String(movie.durationMinutes) : '',
    trailerUrl: movie.trailerUrl || '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (field, value) => setForm((prev) => ({ ...prev, [field]: value }))

  const handleSave = async (e) => {
    e.preventDefault()
    setError('')

    try {
      setIsSubmitting(true)

      const payload = {
        title: form.title,
        description: form.description,
        age_rating: form.age_rating,
        release_date: form.release_date,
        status: form.status,
        genre: form.genre,
        duration_minutes: Number(form.duration_minutes),
        trailerUrl: form.trailerUrl,
      }

      const updated = await updateMovie(movie.id, payload)
      onSaved(updated)
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to update movie.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className='fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4'>
      <div className='bg-gray-900 border border-primary/20 rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6'>
        <h2 className='text-lg font-semibold mb-4'>Edit Movie #{movie.id}</h2>

        <form onSubmit={handleSave} className='grid grid-cols-1 sm:grid-cols-2 gap-4'>
          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Title *</label>
            <input value={form.title} onChange={(e) => handleChange('title', e.target.value)} className={inputClass} required />
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Release Date *</label>
            <input type='date' value={form.release_date} onChange={(e) => handleChange('release_date', e.target.value)} className={inputClass} required />
          </div>

          <div className='sm:col-span-2 space-y-1'>
            <label className='text-xs text-gray-400'>Description *</label>
            <textarea value={form.description} onChange={(e) => handleChange('description', e.target.value)} rows={3} className={inputClass} required />
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Age Rating *</label>
            <select value={form.age_rating} onChange={(e) => handleChange('age_rating', e.target.value)} className={inputClass} required>
              <option value=''>Select</option>
              {ageRatingOptions.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Status *</label>
            <select value={form.status} onChange={(e) => handleChange('status', e.target.value)} className={inputClass} required>
              <option value=''>Select</option>
              {movieStatusOptions.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Genre *</label>
            <select value={form.genre} onChange={(e) => handleChange('genre', e.target.value)} className={inputClass} required>
              <option value=''>Select</option>
              {movieGenreOptions.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Duration (minutes) *</label>
            <input type='number' min='1' value={form.duration_minutes} onChange={(e) => handleChange('duration_minutes', e.target.value)} className={inputClass} required />
          </div>

          <div className='sm:col-span-2 space-y-1'>
            <label className='text-xs text-gray-400'>Trailer URL *</label>
            <input value={form.trailerUrl} onChange={(e) => handleChange('trailerUrl', e.target.value)} className={inputClass} required />
          </div>

          {error && (
            <p className='sm:col-span-2 text-sm text-red-400'>{error}</p>
          )}

          <div className='sm:col-span-2 flex gap-3 justify-end'>
            <button
              type='button'
              onClick={onClose}
              className='px-4 py-2 rounded-md border border-white/20 text-sm hover:bg-white/5 transition'
            >
              Cancel
            </button>
            <button
              type='submit'
              disabled={isSubmitting}
              className='px-4 py-2 rounded-md bg-primary text-black text-sm font-medium hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed'
            >
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

const ListMovie = () => {
  const [movies, setMovies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionSuccess, setActionSuccess] = useState('')
  const [deletingId, setDeletingId] = useState(null)
  const [editingMovie, setEditingMovie] = useState(null)

  useEffect(() => {
    const fetchMovies = async () => {
      try {
        setLoading(true)
        setError('')
        const data = await getAllMoviesForAdmin()
        setMovies(Array.isArray(data) ? data : [])
      } catch (err) {
        setError(err.message || 'Failed to load movies.')
      } finally {
        setLoading(false)
      }
    }

    fetchMovies()
  }, [])

  const normalizedMovies = useMemo(() => {
    return movies.map((movie, index) => ({
      id: movie.movie_id ?? movie.movieId ?? movie.id ?? index,
      title: movie.title ?? 'N/A',
      genre: movie.genre ?? 'N/A',
      ageRating: movie.age_rating ?? movie.ageRating ?? 'N/A',
      durationMinutes: movie.duration_minutes ?? movie.durationMinutes ?? 'N/A',
      releaseDate: movie.release_date ?? movie.releaseDate ?? '',
      status: movie.status ?? 'N/A',
      trailerUrl: movie.trailerUrl ?? movie.trailer_url ?? '',
      description: movie.description ?? '',
    }))
  }, [movies])

  const handleDeleteMovie = async (movieId, movieTitle) => {
    if (!window.confirm(`Are you sure you want to delete movie "${movieTitle}" (#${movieId})?`)) return

    try {
      setDeletingId(movieId)
      setActionError('')
      setActionSuccess('')

      await deleteMovie(movieId)

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

  const handleMovieSaved = (updatedMovie) => {
    setMovies((prev) =>
      prev.map((m) => {
        const id = m.movie_id ?? m.movieId ?? m.id
        if (id === (updatedMovie.movie_id ?? updatedMovie.movieId ?? updatedMovie.id)) {
          return { ...m, ...updatedMovie }
        }
        return m
      })
    )
    setActionSuccess('Movie updated successfully.')
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
                        {movie.durationMinutes !== 'N/A' ? `${movie.durationMinutes} min` : 'N/A'}
                      </td>
                      <td className='px-4 py-3'>{formatDate(movie.releaseDate)}</td>
                      <td className='px-4 py-3'>{formatValue(movie.status)}</td>
                      <td className='px-4 py-3'>
                        {movie.trailerUrl ? (
                          <a href={movie.trailerUrl} target='_blank' rel='noreferrer' className='text-primary underline'>
                            Open
                          </a>
                        ) : 'N/A'}
                      </td>
                      <td className='px-4 py-3'>
                        <div className='flex gap-2'>
                          <button
                            onClick={() => { setActionError(''); setActionSuccess(''); setEditingMovie(movie) }}
                            className='rounded-md bg-blue-600 px-3 py-1.5 text-white text-xs hover:bg-blue-700'
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteMovie(movie.id, movie.title)}
                            disabled={deletingId === movie.id}
                            className='rounded-md bg-red-600 px-3 py-1.5 text-white text-xs hover:bg-red-700 disabled:opacity-60 disabled:cursor-not-allowed'
                          >
                            {deletingId === movie.id ? 'Deleting...' : 'Delete'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {editingMovie && (
        <EditMovieModal
          movie={editingMovie}
          onClose={() => setEditingMovie(null)}
          onSaved={handleMovieSaved}
        />
      )}
    </div>
  )
}

export default ListMovie
