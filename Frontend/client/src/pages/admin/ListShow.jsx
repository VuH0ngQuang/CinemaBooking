import React, { useEffect, useMemo, useState } from 'react'
import Title from '../../components/admin/Title'
import { getAllShowtimes, getMovies, getScreeningRooms, deleteShowtime } from '../../lib/showtimeApi'
import { useAuth } from '../../context/AuthContext'

const formatDateTime = (value) => {
  if (!value) return 'N/A'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

const ListShow = () => {
  const { token } = useAuth()

  const [showtimes, setShowtimes] = useState([])
  const [movies, setMovies] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionSuccess, setActionSuccess] = useState('')
  const [deletingId, setDeletingId] = useState(null)

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

  const handleDeleteShowtime = async (showtimeId) => {
    const confirmed = window.confirm(`Are you sure you want to delete showtime #${showtimeId}?`)

    if (!confirmed) return

    try {
      setDeletingId(showtimeId)
      setActionError('')
      setActionSuccess('')

      await deleteShowtime(showtimeId, token)

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
                        <button
                          onClick={() => handleDeleteShowtime(showtime.id)}
                          disabled={deletingId === showtime.id}
                          className='rounded-md bg-red-600 px-3 py-1.5 text-white hover:bg-red-700 disabled:opacity-60 disabled:cursor-not-allowed'
                        >
                          {deletingId === showtime.id ? 'Deleting...' : 'Delete'}
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

export default ListShow