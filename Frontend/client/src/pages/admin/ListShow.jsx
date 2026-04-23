import React, { useEffect, useMemo, useState } from 'react'
import Title from '../../components/admin/Title'
import { getAllShowtimes, getMovies, getScreeningRooms, deleteShowtime, updateShowtime } from '../../lib/showtimeApi'

const showtimeStatusOptions = ['SCHEDULED', 'ONGOING', 'ENDED', 'CANCELLED']

const inputClass = 'w-full rounded-md bg-black/30 border border-white/15 px-3 py-2 outline-none text-sm'

const toLocalDateTimeInput = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const EditShowtimeModal = ({ showtime, movies, rooms, onClose, onSaved }) => {
  const [form, setForm] = useState({
    status: showtime.status !== 'N/A' ? showtime.status : '',
    start_time: toLocalDateTimeInput(showtime.startTime),
    end_time: toLocalDateTimeInput(showtime.endTime),
    seat_price: showtime.seatPrice !== '' ? String(showtime.seatPrice) : '',
    buffer_time: showtime.bufferTime !== '' ? String(showtime.bufferTime) : '',
    movie_id: showtime.movieId !== null ? String(showtime.movieId) : '',
    screening_room_id: showtime.roomId !== null ? String(showtime.roomId) : '',
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
        status: form.status,
        start_time: form.start_time ? `${form.start_time}:00` : undefined,
        end_time: form.end_time ? `${form.end_time}:00` : undefined,
        seat_price: Number(form.seat_price),
        buffer_time: Number(form.buffer_time),
        movie_id: Number(form.movie_id),
        screening_room_id: Number(form.screening_room_id),
      }

      const updated = await updateShowtime(showtime.id, payload)
      onSaved(updated)
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to update showtime.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className='fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4'>
      <div className='bg-gray-900 border border-primary/20 rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6'>
        <h2 className='text-lg font-semibold mb-4'>Edit Showtime #{showtime.id}</h2>

        <form onSubmit={handleSave} className='grid grid-cols-1 sm:grid-cols-2 gap-4'>
          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Status *</label>
            <select value={form.status} onChange={(e) => handleChange('status', e.target.value)} className={inputClass} required>
              <option value=''>Select</option>
              {showtimeStatusOptions.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Seat Price *</label>
            <input type='number' min='0' value={form.seat_price} onChange={(e) => handleChange('seat_price', e.target.value)} className={inputClass} required />
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Start Time *</label>
            <input type='datetime-local' value={form.start_time} onChange={(e) => handleChange('start_time', e.target.value)} className={inputClass} required />
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>End Time *</label>
            <input type='datetime-local' value={form.end_time} onChange={(e) => handleChange('end_time', e.target.value)} className={inputClass} required />
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Buffer Time (min) *</label>
            <input type='number' min='0' value={form.buffer_time} onChange={(e) => handleChange('buffer_time', e.target.value)} className={inputClass} required />
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Movie *</label>
            <select value={form.movie_id} onChange={(e) => handleChange('movie_id', e.target.value)} className={inputClass} required>
              <option value=''>Select</option>
              {movies.map((movie, i) => {
                const id = movie.movie_id ?? movie.id ?? i
                return <option key={id} value={id}>{movie.title ?? `Movie #${id}`}</option>
              })}
            </select>
          </div>

          <div className='space-y-1'>
            <label className='text-xs text-gray-400'>Screening Room *</label>
            <select value={form.screening_room_id} onChange={(e) => handleChange('screening_room_id', e.target.value)} className={inputClass} required>
              <option value=''>Select</option>
              {rooms.map((room, i) => {
                const id = room.screening_room_id ?? room.id ?? i
                return <option key={id} value={id}>{room.room_name ?? room.name ?? `Room #${id}`}</option>
              })}
            </select>
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

const formatDateTime = (value) => {
  if (!value) return 'N/A'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

const ListShow = () => {
  const [showtimes, setShowtimes] = useState([])
  const [movies, setMovies] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionSuccess, setActionSuccess] = useState('')
  const [deletingId, setDeletingId] = useState(null)
  const [editingShowtime, setEditingShowtime] = useState(null)

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        setError('')

        const [showtimesData, moviesData, roomsData] = await Promise.all([
          getAllShowtimes(),
          getMovies(),
          getScreeningRooms(),
        ])

        setShowtimes(Array.isArray(showtimesData) ? showtimesData : [])
        setMovies(Array.isArray(moviesData) ? moviesData : [])
        setRooms(Array.isArray(roomsData) ? roomsData : [])
      } catch (err) {
        setError(err.message || 'Failed to load showtimes.')
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  const movieMap = useMemo(() => {
    const map = {}

    movies.forEach((movie, index) => {
      const movieId = movie.movie_id ?? movie.id ?? index
      map[movieId] = movie.title ?? movie.name ?? `Movie #${movieId}`
    })

    return map
  }, [movies])

  const roomMap = useMemo(() => {
    const map = {}

    rooms.forEach((room, index) => {
      const roomId = room.screening_room_id ?? room.id ?? index
      map[roomId] = room.room_name ?? room.name ?? `Room #${roomId}`
    })

    return map
  }, [rooms])

  const normalizedShowtimes = useMemo(() => {
    return showtimes.map((showtime, index) => {
      const showtimeId = showtime.showtime_id ?? showtime.id ?? index
      const movieId = showtime.movie_id ?? showtime.movieId ?? null
      const roomId = showtime.screening_room_id ?? showtime.room_id ?? showtime.screeningRoomId ?? null

      return {
        id: showtimeId,
        movieId,
        roomId,
        movieTitle: movieMap[movieId] || `Movie #${movieId ?? 'N/A'}`,
        roomName: roomMap[roomId] || `Room #${roomId ?? 'N/A'}`,
        startTime: showtime.start_time ?? showtime.startTime ?? '',
        endTime: showtime.end_time ?? showtime.endTime ?? '',
        seatPrice: showtime.seat_price ?? showtime.seatPrice ?? '',
        status: showtime.status ?? 'N/A',
        bufferTime: showtime.buffer_time ?? showtime.bufferTime ?? '',
      }
    })
  }, [showtimes, movieMap, roomMap])

  const handleShowtimeSaved = (updatedShowtime) => {
    setShowtimes((prev) =>
      prev.map((s) => {
        const id = s.showtime_id ?? s.id
        if (id === (updatedShowtime.showtime_id ?? updatedShowtime.id)) {
          return { ...s, ...updatedShowtime }
        }
        return s
      })
    )
    setActionSuccess('Showtime updated successfully.')
  }

  const handleDeleteShowtime = async (showtimeId) => {
    const confirmed = window.confirm(`Are you sure you want to delete showtime #${showtimeId}?`)

    if (!confirmed) return

    try {
      setDeletingId(showtimeId)
      setActionError('')
      setActionSuccess('')

      await deleteShowtime(showtimeId)

      setShowtimes((prev) =>
        prev.filter((showtime) => {
          const currentId = showtime.showtime_id ?? showtime.id
          return currentId !== showtimeId
        })
      )

      setActionSuccess(`Showtime #${showtimeId} deleted successfully.`)
    } catch (err) {
      setActionError(err.message || 'Failed to delete showtime.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className='space-y-6'>
      <Title text1='List' text2='Shows' />

      {loading && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 p-4 text-sm text-gray-300'>
          Loading showtimes...
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
                  <th className='px-4 py-3'>Showtime ID</th>
                  <th className='px-4 py-3'>Movie</th>
                  <th className='px-4 py-3'>Room</th>
                  <th className='px-4 py-3'>Start Time</th>
                  <th className='px-4 py-3'>End Time</th>
                  <th className='px-4 py-3'>Seat Price</th>
                  <th className='px-4 py-3'>Buffer</th>
                  <th className='px-4 py-3'>Status</th>
                  <th className='px-4 py-3'>Actions</th>
                </tr>
              </thead>

              <tbody>
                {normalizedShowtimes.length === 0 ? (
                  <tr>
                    <td colSpan='9' className='px-4 py-6 text-center text-gray-400'>
                      No showtimes found.
                    </td>
                  </tr>
                ) : (
                  normalizedShowtimes.map((showtime) => (
                    <tr
                      key={showtime.id}
                      className='border-t border-white/10 hover:bg-white/5 transition-colors'
                    >
                      <td className='px-4 py-3'>{showtime.id}</td>
                      <td className='px-4 py-3'>{showtime.movieTitle}</td>
                      <td className='px-4 py-3'>{showtime.roomName}</td>
                      <td className='px-4 py-3'>{formatDateTime(showtime.startTime)}</td>
                      <td className='px-4 py-3'>{formatDateTime(showtime.endTime)}</td>
                      <td className='px-4 py-3'>{showtime.seatPrice || 'N/A'}</td>
                      <td className='px-4 py-3'>{showtime.bufferTime || 'N/A'}</td>
                      <td className='px-4 py-3'>{showtime.status}</td>
                      <td className='px-4 py-3'>
                        <div className='flex gap-2'>
                          <button
                            onClick={() => { setActionError(''); setActionSuccess(''); setEditingShowtime(showtime) }}
                            className='rounded-md bg-blue-600 px-3 py-1.5 text-white text-xs hover:bg-blue-700'
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteShowtime(showtime.id)}
                            disabled={deletingId === showtime.id}
                            className='rounded-md bg-red-600 px-3 py-1.5 text-white text-xs hover:bg-red-700 disabled:opacity-60 disabled:cursor-not-allowed'
                          >
                            {deletingId === showtime.id ? 'Deleting...' : 'Delete'}
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

      {editingShowtime && (
        <EditShowtimeModal
          showtime={editingShowtime}
          movies={movies}
          rooms={rooms}
          onClose={() => setEditingShowtime(null)}
          onSaved={handleShowtimeSaved}
        />
      )}
    </div>
  )
}

export default ListShow