import React, { useEffect, useMemo, useState } from 'react'
import Title from '../../components/admin/Title'
import {
  getAllBookings,
  getAllMoviesForAdmin,
  getAllShowtimesForAdmin,
  getAllUsers,
} from '../../lib/adminManagementApi'

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return 'N/A'
  return String(value)
}

const formatDateTime = (value) => {
  if (!value) return 'N/A'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)

  return date.toLocaleString()
}

const ListBooking = () => {
  const [bookings, setBookings] = useState([])
  const [users, setUsers] = useState([])
  const [showtimes, setShowtimes] = useState([])
  const [movies, setMovies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        setError('')

        const [bookingsData, usersData, showtimesData, moviesData] = await Promise.all([
          getAllBookings(),
          getAllUsers(),
          getAllShowtimesForAdmin(),
          getAllMoviesForAdmin(),
        ])

        setBookings(Array.isArray(bookingsData) ? bookingsData : [])
        setUsers(Array.isArray(usersData) ? usersData : [])
        setShowtimes(Array.isArray(showtimesData) ? showtimesData : [])
        setMovies(Array.isArray(moviesData) ? moviesData : [])
      } catch (err) {
        setError(err.message || 'Failed to load bookings.')
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  const userMap = useMemo(() => {
    const map = {}

    users.forEach((user, index) => {
      const userId = user.user_id ?? user.userId ?? user.id ?? index
      map[userId] = {
        fullName: user.full_name ?? user.fullName ?? 'N/A',
        email: user.email ?? 'N/A',
      }
    })

    return map
  }, [users])

  const movieMap = useMemo(() => {
    const map = {}

    movies.forEach((movie, index) => {
      const movieId = movie.movie_id ?? movie.movieId ?? movie.id ?? index
      map[movieId] = movie.title ?? movie.name ?? `Movie #${movieId}`
    })

    return map
  }, [movies])

  const showtimeMap = useMemo(() => {
    const map = {}

    showtimes.forEach((showtime, index) => {
      const showtimeId = showtime.showtime_id ?? showtime.showtimeId ?? showtime.id ?? index
      const movieId = showtime.movie_id ?? showtime.movieId ?? null

      map[showtimeId] = {
        movieId,
        startTime: showtime.start_time ?? showtime.startTime ?? '',
        endTime: showtime.end_time ?? showtime.endTime ?? '',
        status: showtime.status ?? 'N/A',
      }
    })

    return map
  }, [showtimes])

  const normalizedBookings = useMemo(() => {
    return bookings.map((booking, index) => {
      const bookingId = booking.booking_id ?? booking.bookingId ?? booking.id ?? index
      const userId = booking.user_id ?? booking.userId ?? null
      const showtimeId = booking.showtime_id ?? booking.showtimeId ?? null
      const totalPrice = booking.total_price ?? booking.totalPrice ?? null
      const bookingStatus =
        booking.booking_status ??
        booking.bookingStatus ??
        booking.status ??
        'N/A'
      const createdAt = booking.created_at ?? booking.createdAt ?? ''
      const confirmedAt = booking.confirmed_at ?? booking.confirmedAt ?? ''
      const expiredAt = booking.expired_at ?? booking.expiredAt ?? ''
      const canceledAt = booking.canceled_at ?? booking.canceledAt ?? ''

      const userInfo = userMap[userId] || {
        fullName: `User #${userId ?? 'N/A'}`,
        email: 'N/A',
      }

      const showtimeInfo = showtimeMap[showtimeId] || {
        movieId: null,
        startTime: '',
        endTime: '',
        status: 'N/A',
      }

      const movieTitle = movieMap[showtimeInfo.movieId] || `Movie #${showtimeInfo.movieId ?? 'N/A'}`

      return {
        bookingId,
        userName: userInfo.fullName,
        userEmail: userInfo.email,
        movieTitle,
        showtimeText: showtimeInfo.startTime
          ? `${formatDateTime(showtimeInfo.startTime)}`
          : 'N/A',
        totalPrice,
        bookingStatus,
        showtimeStatus: showtimeInfo.status,
        createdAt,
        confirmedAt,
        expiredAt,
        canceledAt,
      }
    })
  }, [bookings, movieMap, showtimeMap, userMap])

  return (
    <div className='space-y-6'>
      <Title text1='List' text2='Bookings' />

      {loading && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 p-4 text-sm text-gray-300'>
          Loading bookings...
        </div>
      )}

      {error && (
        <div className='rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300'>
          {error}
        </div>
      )}

      {!loading && !error && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 overflow-hidden'>
          <div className='overflow-x-auto'>
            <table className='w-full text-sm'>
              <thead className='bg-black/30 text-left'>
                <tr>
                  <th className='px-4 py-3'>Booking ID</th>
                  <th className='px-4 py-3'>User</th>
                  <th className='px-4 py-3'>Email</th>
                  <th className='px-4 py-3'>Movie</th>
                  <th className='px-4 py-3'>Showtime</th>
                  <th className='px-4 py-3'>Total Price</th>
                  <th className='px-4 py-3'>Booking Status</th>
                  <th className='px-4 py-3'>Showtime Status</th>
                  <th className='px-4 py-3'>Created At</th>
                  <th className='px-4 py-3'>Confirmed At</th>
                  <th className='px-4 py-3'>Expired At</th>
                  <th className='px-4 py-3'>Canceled At</th>
                </tr>
              </thead>

              <tbody>
                {normalizedBookings.length === 0 ? (
                  <tr>
                    <td colSpan='12' className='px-4 py-6 text-center text-gray-400'>
                      No bookings found.
                    </td>
                  </tr>
                ) : (
                  normalizedBookings.map((booking) => (
                    <tr
                      key={booking.bookingId}
                      className='border-t border-white/10 hover:bg-white/5 transition-colors'
                    >
                      <td className='px-4 py-3'>{formatValue(booking.bookingId)}</td>
                      <td className='px-4 py-3'>{formatValue(booking.userName)}</td>
                      <td className='px-4 py-3 break-all'>{formatValue(booking.userEmail)}</td>
                      <td className='px-4 py-3'>{formatValue(booking.movieTitle)}</td>
                      <td className='px-4 py-3'>{formatValue(booking.showtimeText)}</td>
                      <td className='px-4 py-3'>{formatValue(booking.totalPrice)}</td>
                      <td className='px-4 py-3'>{formatValue(booking.bookingStatus)}</td>
                      <td className='px-4 py-3'>{formatValue(booking.showtimeStatus)}</td>
                      <td className='px-4 py-3'>{formatDateTime(booking.createdAt)}</td>
                      <td className='px-4 py-3'>{formatDateTime(booking.confirmedAt)}</td>
                      <td className='px-4 py-3'>{formatDateTime(booking.expiredAt)}</td>
                      <td className='px-4 py-3'>{formatDateTime(booking.canceledAt)}</td>
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

export default ListBooking
